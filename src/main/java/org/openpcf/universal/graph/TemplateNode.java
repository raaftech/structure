package org.openpcf.universal.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Transaction;
import org.openpcf.universal.base.Context;
import org.openpcf.universal.graph.repositories.TemplateNodeRepository;
import org.openpcf.universal.graph.repositories.TypeNodeRepository;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.core.GraphDatabase;

import java.util.HashSet;
import java.util.Set;

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

    private final static int MAXRECURSION = 50;

    public TemplateNode() {}

    public TemplateNode(String name, String desc) {
        super(name, desc);
    }

    public TemplateNode(String name, String desc, String uuid) {
        super(name, desc, uuid);
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

        json.put("name", getName());
        json.put("desc", getDesc());
        json.put("uuid", getUuid());

        comprises.stream()
                .forEach(template -> {
                    subTemplates.add(template.getTemplateAsJson());
                });

        return json;
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
        return this;
    }
}
