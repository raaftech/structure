package org.openpcf.universal.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Transaction;
import org.openpcf.universal.base.Context;
import org.openpcf.universal.graph.repositories.TemplateNodeRepository;
import org.openpcf.universal.graph.repositories.TypeNodeRepository;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Give a description of TemplateNode here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-03-09
 */
@NodeEntity
public class TemplateNode extends Node implements Template {

    @RelatedTo(type = "ISA", direction = Direction.OUTGOING)
    @Fetch
    TypeNode isa;

    @RelatedTo(type = "HAS_COMPONENT", direction = Direction.OUTGOING)
    @Fetch
    Set<TemplateNode> hasComponents;

    boolean startOfTemplate;
    boolean required;
    boolean unique;

    TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();
    TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();

    public TemplateNode() {}

    public TemplateNode(String name, String desc) {
        super(name, desc);
    }

    public TemplateNode(String name, String desc, String uuid) {
        super(name, desc, uuid);
    }

    public TemplateNode setIsa(TypeNode isa) {
        this.isa = isa;

        return this;
    }

    public TemplateNode setIsa(String isa) {
        TypeNode found = typeNodeRepository.findByUuid(isa);
        if (found != null) {
            setIsa(found);
        } else {
            throw new RuntimeException("Could not find TypeNode " + isa + " to set ISA relation.");
        }

        return this;
    }

    public boolean isStartOfTemplate() {
        return startOfTemplate;
    }

    public TemplateNode setStartOfTemplate(boolean startOfTemplate) {
        this.startOfTemplate = startOfTemplate;

        return this;
    }

    public boolean isRequired() {
        return required;
    }

    public TemplateNode setRequired(boolean required) {
        this.required = required;

        return this;
    }

    public boolean isUnique() {
        return unique;
    }

    public TemplateNode setUnique(boolean unique) {
        this.unique = unique;

        return this;
    }

    public TemplateNode addComponent(TemplateNode component) {
        if (hasComponents == null) {
            hasComponents = new HashSet<>();
        }
        hasComponents.add(component);

        return this;
    }

    public boolean removeComponent(TemplateNode component) {
        List<TemplateNode> found = hasComponents.stream()
                .filter(c -> c.getUuid() == component.getUuid())
                .collect(Collectors.toList());
        return hasComponents.remove(found.get(0));
    }

    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();

        json.put("name", getName());
        json.put("desc", getDesc());
        json.put("uuid", getUuid());
        json.put("isa", isa.getUuid());
        try {
            json.put("schema", mapper.writeValueAsString(isa.getSchema()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        if (hasComponents.size() > 0) {
            ArrayNode children = json.putArray("children");
            hasComponents.forEach(component -> {
                children.add(component.toJson());
            });
        }

        return json;
    }

    public String instantiate(TemplateNode template, InstanceNode parent, JsonNode json) {
        if (!startOfTemplate && parent == null) {
            throw new RuntimeException("Cannot instantiate a TemplateNode that is not the start of a template without a parent.");
        }

        TypeNode group = template.isa;
        JsonNode groupInstanceData = json.get(group.getUuid());
        String groupInstanceUuid = null;

        if (groupInstanceData != null) {
            final InstanceNode groupInstanceNode = new InstanceNode(groupInstanceData);
            groupInstanceUuid = groupInstanceNode.getUuid();
            if (parent != null) parent.addComponent(groupInstanceNode);

            template.hasComponents.stream()
                    .forEach(t -> {
                        JsonNode newInstanceData = json.get(t.isa.getUuid());
                        if (newInstanceData != null) {
                            t.instantiate(t, groupInstanceNode, json);
                        } else {
                            throw new RuntimeException("Couldn't get data for TypeNode " + t.getName() + "(" + t.getUuid() + ") to instantiate TemplateNode");
                        }
                    });

            groupInstanceNode.save();
        } else {
            throw new RuntimeException("Couldn't get data for TypeNode " + template.isa.getName() + "(" + template
                    .isa.getUuid() + ") to instantiate TemplateNode");
        }

        return groupInstanceUuid;
    }

    public TemplateNode save() {
        Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
        TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();

        try {
            templateNodeRepository.save(this);
            tx.success();
        } catch (Exception e) {
            e.printStackTrace();
            tx.failure();
        } finally {
            tx.close();
        }

        return this;
    }
}
