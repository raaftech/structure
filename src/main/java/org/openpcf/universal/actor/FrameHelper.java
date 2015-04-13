// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.actor;

import java.util.Set;

import org.neo4j.graphdb.Transaction;
import org.openpcf.universal.base.BaseActor;
import org.openpcf.universal.base.Context;
import org.openpcf.universal.graph.InstanceNode;
import org.openpcf.universal.graph.TemplateNode;
import org.openpcf.universal.graph.TypeNode;
import org.openpcf.universal.graph.repositories.InstanceNodeRepository;
import org.openpcf.universal.graph.repositories.TemplateNodeRepository;
import org.openpcf.universal.graph.repositories.TypeNodeRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
                String node = getArgs(args, "node", "");
                TemplateNode found = null;

                if (isUuid(node)) {
                    found = templateNodeRepository.findByUuid(node);
                }

                if (found == null) {
                    result.put("error", "Couldn't find TemplateNode.");
                    return response(request, result);
                } else {
                    return response(request, found.toJson());
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
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");
                String isa = request.get("nodeuuid").asText();

                Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
                try {
                    InstanceNode newInstanceNode = new InstanceNode(args, isa);
                    newInstanceNode.save();
                    tx.success();
                    result.put("uuid", newInstanceNode.getUuid());
                } catch (RuntimeException e) {
                    tx.failure();
                    e.printStackTrace();
                    result.put("error", "Error creating new InstanceNode.");
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

    public MessageHandler createtemplatenode() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");

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

                String nodeUuid = getArgs(args, "nodeUuid", "");
                String templateUuid = getArgs(args, "templateUuid", "");

                TemplateNode parent = templateNodeRepository.findByUuid(templateUuid);
                TemplateNode node = templateNodeRepository.findByUuid(nodeUuid);

                Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
                try {
                    if (parent != null) {
                        if (parent.removeComponent(node)) {
                            templateNodeRepository.delete(node);
                            parent.save();
                            result.put("information", "Removed node " + nodeUuid + " from TemplateNode " + templateUuid);
                        } else {
                            result.put("error", "Didn't remove node " + nodeUuid + " from TemplateNode " + templateUuid + ", " +
                                    "rolled back transaction.");
                            tx.failure();
                        }
                    }
                    tx.success();
                } catch (Exception e) {
                    tx.failure();
                    e.printStackTrace();
                    result.put("error", e.getMessage());
                } finally {
                    tx.close();
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
                JsonNode nodes = getArgs(args, "nodes", (JsonNode)null);
                String template = getArgs(args, "template", "ERR_INVALID");

                TemplateNode templateNode = templateNodeRepository.findByUuid(template);

                if (templateNode != null && nodes != null) {
                    Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
                    try {
                        result.put("uuid", templateNode.instantiate(templateNode, null, nodes, 0));
                        tx.success();
                        result.put("information", "Successfully instantiated TemplateNode " + template);
                    } catch (Exception e) {
                        e.printStackTrace();
                        tx.failure();
                        result.put("error", "Error while instantiating TemplateNode " + template + ", transaction failed.");
                    } finally {
                        tx.close();
                    }
                } else {
                    result.put("error", "No template UUID or instantiation data found in request.");
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
