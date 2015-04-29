// Copyright © 2008-2014 RAAF Technology bv

package com.raaftech.structure.actor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.neo4j.graphdb.Transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.BaseActor;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.TemplateNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import com.raaftech.structure.graph.repositories.TemplateNodeRepository;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;

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
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                TypeNode found = null;
                JsonNode args = request.get("args");
                JsonNode schema = jsonFromResource("schema/framehelper/getnode.schema.json");
                if (isValidJson(schema, args)) {
                    String node = getArgs(args, "node", "Component");

                    if (isUuid(node)) {
                        found = typeNodeRepository.findByUuid(node);
                    } else {
                        found = typeNodeRepository.findByName(node);
                    }

                    if (found == null) {
                        result.put("error", "Couldn't find TypeNode.");
                    } else {
                        result = found.toJson();
                    }
                } else {
                    result.put("error", "JSON didn't pass validation.");
                }
                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/framehelper.component.json");
            }
        };
    }

    public MessageHandler instancenode() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                InstanceNode found = null;
                JsonNode args = request.get("args");
                JsonNode schema = jsonFromResource("schema/framehelper/getnode.schema.json");
                if (isValidJson(schema, args)) {
                    String node = getArgs(args, "node", "Component");

                    if (isUuid(node)) {
                        found = instanceNodeRepository.findByUuid(node);
                    }

                    if (found == null) {
                        result.put("error", "Couldn't find InstanceNode.");
                    } else {
                        result = found.toJson();
                    }
                } else {
                    result.put("error", "JSON didn't pass validation.");
                }
                return response(request, result);
            }

            @Override
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
                JsonNode schema = jsonFromResource("schema/framehelper/getnode.schema.json");
                if (isValidJson(schema, args)) {
                    String node = getArgs(args, "node", "");
                    TemplateNode found = null;

                    if (isUuid(node)) {
                        found = templateNodeRepository.findByUuid(node);
                    }

                    if (found == null) {
                        result.put("error", "Couldn't find TemplateNode.");
                    } else {
                        result = found.toJson();
                    }
                } else {
                    result.put("error", "JSON didn't pass validation.");
                }
                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    public MessageHandler createinstancenode() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");
                JsonNode schema = jsonFromResource("schema/framehelper/createinstancenode.schema.json");
                if (isValidJson(schema, args)) {
                    String isa = args.get("node").asText();

                    Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
                    try {
                        InstanceNode newInstanceNode = new InstanceNode((ObjectNode)args, isa);
                        newInstanceNode.save();
                        tx.success();
                        result.put("uuid", newInstanceNode.getUuid());
                    } catch (RuntimeException e) {
                        tx.failure();
                        e.printStackTrace();
                        result.put("error", e.getMessage());
                    } finally {
                        tx.close();
                    }
                } else {
                    result.put("error", "JSON didn't pass validation.");
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return JsonNodeFactory.instance.objectNode();
            }
        };
    }

    public MessageHandler createtemplatenode() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");

                JsonNode schema = jsonFromResource("schema/framehelper/createtemplatenode.schema.json");
                if (isValidJson(schema, args)) {
                    // We're setting the default values to ERR_INVALID.
                    // The values don't really matter, as long as instanceOfUuid is not
                    // set to a valid and existing UUID.
                    String parent = getArgs(args, "parent", "");
                    String name = getArgs(args, "name", "");
                    String desc = getArgs(args, "desc", "");
                    String isa = getArgs(args, "isa", "");
                    // We're using Boolean.parseBoolean, so this will evaluate to true only when the
                    // string has value true (case ignored). Anything else will evaluate to false.
                    boolean unique = Boolean.parseBoolean(getArgs(args, "unique", "ERR_INVALID"));
                    boolean required = Boolean.parseBoolean(getArgs(args, "required", "ERR_INVALID"));
                    boolean start = Boolean.parseBoolean(getArgs(args, "start", "ERR_INVALID"));

                    try {
                        if (!parent.equals("") && start == true) {
                            throw new RuntimeException("Cannot create TemplateNode that's both the start of a template and has a parent " +
                                    "TemplateNode.");
                        }
                        if (parent.equals("") && start == false) {
                            throw new RuntimeException("Cannot create TemplateNode that has no parent and isn't the start of a template.");
                        }
                        if (name.equals("") || desc.equals("") || isa.equals("")) {
                            throw new RuntimeException("Did not get valid name, description or ISA for new TemplateNode.");
                        }

                        TypeNode isaTypeNode = typeNodeRepository.findByUuid(isa);
                        if (isaTypeNode != null) {
                            TemplateNode newTemplateNode = new TemplateNode(name, desc);
                            newTemplateNode.setIsa(isaTypeNode)
                            .setStartOfTemplate(start)
                            .setRequired(required)
                            .setUnique(unique);

                            if (!parent.equals("")) {
                                TemplateNode parentTemplateNode = templateNodeRepository.findByUuid(parent);
                                if (parentTemplateNode != null) {
                                    parentTemplateNode.addComponent(newTemplateNode);
                                    parentTemplateNode.save();
                                } else {
                                    throw new RuntimeException("Cannot create TemplateNode, given parent was not found in the database.");
                                }
                            }
                            newTemplateNode.save();

                            result.put("uuid", newTemplateNode.getUuid());
                        } else {
                            throw new RuntimeException("Could not find TypeNode by which to group the Template.");
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        result.put("error", e.getMessage());
                    }
                } else {
                    result.put("error", "JSON didn't pass validation.");
                }

                return response(request, result);
            }

            @Override
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
                ArrayNode templateNodes = result.putArray("templates");
                Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
                Set<TemplateNode> allTemplateNodes = templateNodeRepository.findAllTemplates();

                try {
                    allTemplateNodes.forEach(res -> {
                        if (res.getClass() == TemplateNode.class) {
                            templateNodes.add(res.toJson());
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

    public MessageHandler removenodefromtemplate() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");

                JsonNode schema = jsonFromResource("schema/framehelper/removenodefromtemplate.schema.json");
                if (isValidJson(schema, args)) {
                    String nodeUuid = getArgs(args, "node", "");

                    TemplateNode node = templateNodeRepository.findByUuid(nodeUuid);
                    if (node == null) {
                        result.put("error", "Could not find TemplateNode " + nodeUuid);
                        return response(request, result);
                    }
                    TemplateNode parent = node.getParentComponent();

                    Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
                    try {
                        if (parent != null) {
                            String templateUuid = parent.getUuid();
                            if (parent.removeComponent(node)) {
                                templateNodeRepository.delete(node);
                                parent.save();
                                result.put("information", "Removed node " + nodeUuid + " from TemplateNode " + templateUuid);
                            } else {
                                result.put("error", "Didn't remove node " + nodeUuid + " from TemplateNode " + templateUuid + ", " +
                                        "rolled back transaction.");
                                tx.failure();
                            }
                        } else {
                            long components = node.removeAllComponents() + 1;
                            templateNodeRepository.delete(node);
                            result.put("information", "Removed TemplateNode " + node.getUuid() + " and its children.");
                        }
                        tx.success();
                    } catch (Exception e) {
                        tx.failure();
                        e.printStackTrace();
                        result.put("error", e.getMessage());
                    } finally {
                        tx.close();
                    }
                } else {
                    result.put("error", "JSON didn't pass validation.");
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    public MessageHandler instantiatetemplate() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");

                // validate args
                JsonNode schema = jsonFromResource("schema/framehelper/instantiatetemplate.schema.json");
                if (isValidJson(schema, args)) {
                    JsonNode instancedata = getArgs(args, "instancedata", (JsonNode)null);
                    String template = getArgs(args, "template", "ERR_INVALID");
                    TemplateNode templateNode = templateNodeRepository.findByUuid(template);

                    if (templateNode != null && instancedata != null) {
                        // Map stuff
                        HashMap<String, JsonNode> uuidToDataMap = new HashMap<>();
                        Iterator<JsonNode> elements = instancedata.elements();
                        while (elements.hasNext()) {
                            JsonNode element = elements.next();
                            uuidToDataMap.put(element.get("uuid").asText(), element.get("data"));
                        }

                        Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
                        try {
                            result.put("uuid", templateNode.instantiate(templateNode, null, uuidToDataMap, 0));
                            tx.success();
                        } catch (Exception e) {
                            tx.failure();
                            e.printStackTrace();
                            result.put("error", e.getMessage());
                        } finally {
                            tx.close();
                        }
                    }
                } else {
                    result.put("error", "JSON didn't pass validation.");
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
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");
                String name = getArgs(args, "name", "Component");

                result.put("name", name);
                result.put("cypher", cypherQuery("MATCH (p) WHERE p.name =~ '.*(?i)" + name + ".*' RETURN p"));

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/framehelper.search.json");
            }
        };
    }
}
