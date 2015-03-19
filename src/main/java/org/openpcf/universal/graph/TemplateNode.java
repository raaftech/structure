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
import org.springframework.data.neo4j.core.GraphDatabase;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Give a description of TemplateNode here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-03-09
 */
@NodeEntity
public class TemplateNode extends Node implements Template {

    @RelatedTo(type = "COMPRISES", direction = Direction.OUTGOING)
    private Set<Template> comprises;

    @RelatedTo(type = "GROUPS", direction = Direction.OUTGOING)
    @Fetch
    private TypeNode groupType;

    private final static int MAXRECURSION = 50;

    public TemplateNode() {}

    public TemplateNode(String name, String desc) {
        super(name, desc);
    }

    public TemplateNode(String name, String desc, String uuid) {
        super(name, desc, uuid);
    }

    public TemplateNode(String name, String desc, String uuid, TypeNode groupType) {
        super(name, desc, uuid);
        this.groupType = groupType;
    }

    public TemplateNode(String name, String desc, String uuid, String groupType) {
        super(name, desc, uuid);

        TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();
        TypeNode typeNode = typeNodeRepository.findByUuid(groupType);
        if (typeNode != null) {
            this.groupType = typeNode;
        } else {
            throw new RuntimeException("TypeNode with uuid " + groupType + " not found.");
        }
    }

    public TemplateNode(String name, String desc, TypeNode groupType) {
        this(name, desc, UUID.randomUUID().toString(), groupType);
    }

    private boolean checkRecurseDepth(Set<Template> templates, int depth) {
        if (depth > MAXRECURSION) {
            return false;
        } else {
            boolean ret = true;
            for (Template t: templates) {
                if (t.getClass() == TemplateNode.class) {
                    TemplateNode templateNode = (TemplateNode)t;
                    templateNode.loadTemplate();
                    if (ret == true) {
                        ret = checkRecurseDepth(templateNode.comprises, depth + 1);
                    }
                }
            }
            return ret;
        }
    }

    public void loadTemplate() {
        GraphDatabase graphDatabase = Context.INSTANCE.getGraphDatabase();
        Transaction tx = graphDatabase.beginTx();
        Context.INSTANCE.getNeo4jTemplate().fetch(comprises);
        tx.success();
        tx.close();
    }

    @Override
    public ObjectNode getTemplateAsJson() {
        loadTemplate();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();
        ArrayNode subTemplates = json.putArray("comprises");

        json.put("templateName", getName());
        json.put("templateDesc", getDesc());
        json.put("templateUuid", getUuid());
        json.put("groupName", groupType.getName());
        json.put("groupDesc", groupType.getDesc());
        json.put("groupUuid", groupType.getUuid());
        try {
            json.put("groupSchema", mapper.writeValueAsString(groupType.getSchema()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }


        comprises.stream()
                .forEach(template -> {
                    subTemplates.add(template.getTemplateAsJson());
                });

        return json;
    }

    public TypeNode getGroupType() {
        return groupType;
    }

    public void setGroupType(TypeNode groupType) {
        this.groupType = groupType;
    }

    public Set<TypeNode> getTypeNodes() {
        loadTemplate();
        Set<TypeNode> typeNodes = comprises.stream()
                .filter(t -> t.getClass() == TypeNode.class)
                .map(t -> (TypeNode)t)
                .collect(Collectors.toSet());

        return typeNodes;
    }

    public Set<TemplateNode> getTemplateNodes() {
        loadTemplate();
        Set<TemplateNode> templateNodes = comprises.stream()
                .filter(t -> t.getClass() == TemplateNode.class)
                .map(t -> (TemplateNode)t)
                .collect(Collectors.toSet());

        return templateNodes;
    }

    public String instantiate(TemplateNode template, InstanceNode groupOwner, JsonNode json) {
        template.loadTemplate();
        TypeNode group = template.getGroupType();
        JsonNode groupInstanceData = json.get(group.getUuid());
        String groupInstanceUuid = null;

        if (groupInstanceData != null) {
            final InstanceNode groupInstanceNode = new InstanceNode(groupInstanceData);
            groupInstanceUuid = groupInstanceNode.getUuid();
            if (groupOwner != null) groupOwner.addComponent(groupInstanceNode);

            template.getTypeNodes().stream()
                    .forEach(t -> {
                        JsonNode newInstanceData = json.get(t.getUuid());
                        if (newInstanceData != null) {
                            InstanceNode newInstanceNode = new InstanceNode(newInstanceData);
                            groupInstanceNode.addComponent(newInstanceNode);
                            newInstanceNode.save();
                        } else {
                            throw new RuntimeException("Couldn't get data for TypeNode " + t.getName() + "(" + t.getUuid
                                    () + ") to instantiate TemplateNode");
                        }
                    });

            template.getTemplateNodes().stream()
                    .forEach(t -> {
                        instantiate(t, groupInstanceNode, json);
                    });

            groupInstanceNode.save();
        } else {
            throw new RuntimeException("Couldn't get data for TypeNode " + template.getGroupType().getName() + "(" + template
                    .getGroupType().getUuid() + ") to instantiate TemplateNode");
        }

        return groupInstanceUuid;
    }

    public boolean add(Template template) {
        if (comprises == null) {
            comprises = new HashSet<>();
        }

        Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
        try {
            comprises.add(template);
            tx.success();
        } catch (Exception e) {
            tx.failure();
        } finally {
            tx.close();
        }

        loadTemplate();
        if (!checkRecurseDepth(comprises, 1)) {
            tx = Context.INSTANCE.getGraphDatabase().beginTx();
            try {
                comprises.remove(template);
                tx.success();
            } catch (Exception e) {
                tx.failure();
                e.printStackTrace();
            } finally {
                tx.close();
                return false;
            }
        }
        return true;
    }

    public boolean add(String uuid) {
        TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();
        TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();

        Template template = templateNodeRepository.findByUuid(uuid);
        if (template != null) {
            return add(template);
        } else {
            template = typeNodeRepository.findByUuid(uuid);
            if (template != null) {
                return add(template);
            } else {
                throw new RuntimeException("Couldn't find TemplateNode or TypeNode with uuid " + uuid);
            }
        }
    }

    public boolean remove(Template template) {
        loadTemplate();
        boolean ret = false;
        if (comprises != null) {
            Template found = null;
            for (Template t: comprises) {
                if (t.getUuid() == template.getUuid()) {
                    found = t;
                }
            }

            if (found != null) {
                Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
                try {
                    ret = comprises.remove(found);
                    tx.success();
                } catch (Exception e) {
                    tx.failure();
                } finally {
                    tx.close();
                }
            }
        }
        return ret;
    }

    public boolean remove(String uuid) {
        TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();
        TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();
        if (comprises != null) {
            Template node = typeNodeRepository.findByUuid(uuid);
            if (node == null) {
                node = templateNodeRepository.findByUuid(uuid);
            }

            if (node != null) {
                return remove(node);
            }
        }
        return false;
    }

    public TemplateNode save() {
        if (groupType != null) {
            TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();
            Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
            try {
                templateNodeRepository.save(this);
                tx.success();
            } catch (Exception e) {
                tx.failure();
                throw new RuntimeException("Failed to save TemplateNode");
            } finally {
                tx.close();
            }
        } else {
            throw new RuntimeException("Can't save a TemplateNode that has no GroupType.");
        }
        return this;
    }
}
