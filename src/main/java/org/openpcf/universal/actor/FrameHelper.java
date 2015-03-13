// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.actor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neo4j.graphdb.Transaction;
import org.openpcf.universal.base.BaseActor;
import org.openpcf.universal.base.Context;
import org.openpcf.universal.graph.InstanceNode;
import org.openpcf.universal.graph.Template;
import org.openpcf.universal.graph.TemplateNode;
import org.openpcf.universal.graph.TypeNode;
import org.openpcf.universal.graph.repositories.InstanceNodeRepository;
import org.openpcf.universal.graph.repositories.TemplateNodeRepository;
import org.openpcf.universal.graph.repositories.TypeNodeRepository;
import org.springframework.data.neo4j.conversion.Result;

import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * The FrameHelper actor.
 *
 * @author Maurice Schillemans <maurice.schillemans@raaftech.com>
 * @since 2014-09-24
 */
public class FrameHelper extends BaseActor {

    TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();
    InstanceNodeRepository instanceNodeRepository = Context.INSTANCE.getInstanceNodeRepository();
    TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();

    public MessageHandler typenode() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                TypeNode found = null;
                JsonNode args = request.get("args");
                String node = getArgs(args, "node", "Component");

                if (isUuid(node)) {
                    found = typeNodeRepository.findByUuid(node);
                } else {
                    found = typeNodeRepository.findByName(node);
                }

                if (found == null) {
                    result.put("error", "Couldn't find TypeNode.");
                    return response(request, result);
                } else {
                    return response(request, found.toJson());
                }
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/framehelper.component.json");
            }
        };
    }

    public MessageHandler instancenode() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                InstanceNode found = null;
                JsonNode args = request.get("args");
                String node = getArgs(args, "node", "Component");

                if (isUuid(node)) {
                    found = instanceNodeRepository.findByUuid(node);
                }

                if (found == null) {
                    result.put("error", "Couldn't find InstanceNode.");
                    return response(request, result);
                } else {
                    return response(request, found.toJson());
                }
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/framehelper.node.json");
            }
        };
    }

    public MessageHandler templatenode() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");
                String node = getArgs(args, "node", "");
                TemplateNode found = null;

                if (isUuid(node)) {
                    found = templateNodeRepository.findByUuid(node);
                }

                if (found == null) {
                    result.put("error", "Couldn't find TemplateNode.");
                    return response(request, result);
                } else {
                    return response(request, found.getTemplateAsJson());
                }
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    public MessageHandler createinstancenode() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                TypeNode instanceOf = null;
                JsonNode args = request.get("args");
                LinkedHashMap<String, String> prop = new LinkedHashMap<>();

                // We're setting the default values to ERR_INVALID.
                // The values don't really matter, as long as instanceOfUuid is not
                // set to a valid and existing UUID.
                String name = getArgs(args, "name", "ERR_INVALID");
                String desc = getArgs(args, "desc", "ERR_INVALID");
                String instanceOfUuid = getArgs(args, "instanceofuuid", "ERR_INVALID");
                JsonNode kvs = getArgs(args, "kvs", (JsonNode)null);

                Iterator<String> kvElements = kvs.fieldNames();
                while (kvElements.hasNext()) {
                    String field = kvElements.next();
                    prop.put(field, kvs.get(field).toString());
                }

                Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
                try {
                    InstanceNode newInstanceNode = new InstanceNode(name, desc,
                            instanceOfUuid);
                    newInstanceNode.setProperties(prop);
                    newInstanceNode.save();
                    tx.success();
                    result.put("uuid", newInstanceNode.getUuid());
                } catch (RuntimeException e) {
                    tx.failure();
                    result.put("error", "Error creating new InstanceNode.");
                } finally {
                    tx.close();
                }

                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return JsonNodeFactory.instance.objectNode();
            }
        };
    }

    public MessageHandler createtemplatenode() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");

                // We're setting the default values to ERR_INVALID.
                // The values don't really matter, as long as instanceOfUuid is not
                // set to a valid and existing UUID.
                String name = getArgs(args, "name", "ERR_INVALID");
                String desc = getArgs(args, "desc", "ERR_INVALID");

                if (name.equals("ERR_INVALID") || desc.equals("ERR_INVALID")) {
                    throw new RuntimeException("Did not get valid name and description for new TemplateNode.");
                }

                try {
                    TemplateNode newTemplateNode = new TemplateNode(name, desc);
                    newTemplateNode.save();
                    result.put("uuid", newTemplateNode.getUuid());
                } catch (RuntimeException e) {
                    result.put("error", "Error creating new TemplateNode.");
                }

                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return JsonNodeFactory.instance.objectNode();
            }
        };
    }

    public MessageHandler findalltemplatenodes() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
                Result allTemplateNodes = templateNodeRepository.findAll();

                try {
                    allTemplateNodes.forEach(res -> {
                        if (res.getClass() == TemplateNode.class) {
                            TemplateNode template = (TemplateNode) res;
                            result.put(template.getName(), template.getUuid());
                        }
                    });
                    tx.success();
                } catch (Exception e) {
                    tx.failure();
                    result.put("error", "Error getting all TemplateNodes.");
                } finally {
                    tx.close();
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return JsonNodeFactory.instance.objectNode();
            }
        };
    }

    public MessageHandler addnodetotemplate() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");

                String newUuid = getArgs(args, "uuid", "ERR_INVALID");
                String templateUuid = getArgs(args, "templateUuid", "ERR_INVALID");

                TemplateNode template = templateNodeRepository.findByUuid(templateUuid);
                if (template == null) {
                    result.put("error", "Could not find TemplateNode with uuid " + templateUuid);
                    response(request, result);
                }

                Template newNode = templateNodeRepository.findByUuid(newUuid);
                if (newNode == null) {
                    newNode = typeNodeRepository.findByUuid(newUuid);
                }

                if (newNode != null) {
                    if (template.add(newNode)) {
                        result.put("information", "Added Node " + newUuid + " to TemplateNode " + templateUuid);
                    } else {
                        result.put("error", "Could not add new node to TemplateNode " + templateUuid + ", likely due to depth of " +
                                "recursion.");
                    }
                } else {
                    result.put("error", "Could not add new node to TemplateNode " + templateUuid + ", node " + newUuid + " wasn't" +
                            " found.");
                    response(request, result);
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return JsonNodeFactory.instance.objectNode();
            }
        };
    }

    public MessageHandler removenodefromtemplate() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");

                String nodeUuid = getArgs(args, "nodeUuid", "ERR_INVALID");
                String templateUuid = getArgs(args, "templateUuid", "ERR_INVALID");

                TemplateNode template = templateNodeRepository.findByUuid(templateUuid);
                if (template != null) {
                    if (template.remove(nodeUuid)) {
                        result.put("information", "Removed node " + nodeUuid + " from TemplateNode " + templateUuid);
                    } else {
                        result.put("error", "Didn't remove node " + nodeUuid + " from TemplateNode " + templateUuid);
                    }
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    public MessageHandler search() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");
                String name = getArgs(args, "name", "Component");

                result.put("name", name);
                result.put("cypher", cypherQuery("MATCH (p) WHERE p.name =~ '.*(?i)" + name + ".*' RETURN p"));

                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/framehelper.search.json");
            }
        };
    }
}
