package com.raaftech.structure.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.NodeActor;
import com.raaftech.structure.graph.TemplateNode;
import com.raaftech.structure.graph.repositories.TemplateNodeRepository;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Boy van Duuren &lt;boy@raaftech.com&gt;.
 */

public class TemplateNodeActor extends NodeActor<TemplateNode> {

    private Logger log = LoggerFactory.getLogger(TemplateNodeActor.class);

    public TemplateNodeActor(TemplateNode node) {
        super(node);
    }

    @Override
    protected String getSecondaryAddress() {
        return null;
    }

    // Next methods are exposed on the eventbus

    /**
     * Remove a TemplateNode from a Template, or
     * when the TemplateNode this is called on is the start of
     * a Template, remove the whole template.
     * @return A JSON object with an error field if an error occurred.
     */
    // TODO: Undeploy TemplateNodes when they are removed from the database
    @SuppressWarnings("unused")
    public MessageHandler removeFromTemplate() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");

                TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();
                TemplateNode node = getNode();
                TemplateNode parent = node.getParentComponent();

                Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
                try {
                    if (parent != null) {
                        String templateUuid = parent.getUuid();
                        if (parent.removeComponent(node)) {
                            parent.save();
                            result.put("data", "Removed node " + uuid + " from TemplateNode " + templateUuid);
                        } else {
                            result.put("error", "Didn't remove node " + uuid + " from TemplateNode " + templateUuid + ", " +
                                    "rolled back transaction.");
                            tx.failure();
                        }
                    } else {
                        long components = node.removeAllComponents() + 1;
                        templateNodeRepository.delete(node);
                        result.put("data", "Removed TemplateNode " + uuid + " and its children.");
                    }
                    vertx.undeploy(deploymentID(), res -> {
                        if (res.succeeded()) {
                            log.info("Undeployed template verticle with ID {} on removal from database.", deploymentID());
                        } else {
                            log.error("Couldn't undeploy template verticle {} on removal from database.", deploymentID());
                            log.debug(res.cause().getMessage());
                        }
                    });
                    tx.success();
                } catch (Exception e) {
                    tx.failure();
                    
                    log.error(e.getMessage());

        			if (log.isTraceEnabled()) {
        				e.printStackTrace();
        			}
        			
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

    /**
     * Instantiate a template resulting in a new InstanceNode group.
     * @return A JSON object containing the first InstanceNode in the group's uuid,
     * or a JSON object with an error field when an error occurs.
     */
    @SchemaPath("schema/node/templatenode/instantiate.schema.json")
    @SuppressWarnings("unused")
    public MessageHandler instantiate() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                TemplateNodeRepository templateNodeRepository = Context.INSTANCE.getTemplateNodeRepository();
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");

                JsonNode instancedata = args.get("instancedata");
                TemplateNode templateNode = getNode();

                if (templateNode != null && instancedata != null) {
                    try {
                        String uuid = templateNode.instantiate(instancedata);
                        if (uuid != null) {
                            result.put("data", uuid);
                        } else {
                            result.put("error", "Error while instantiating template.");
                        }
                    } catch (RuntimeException e) {
                    	log.error(e.getMessage());

            			if (log.isTraceEnabled()) {
            				e.printStackTrace();
            			}
            			
            			result.put("error", e.getMessage());
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

    /**
     * Run a discovery routine on a template.
     * @return Nothing really informative... WIP
     */
    @SchemaPath("schema/node/templatenode/discover.schema.json")
    @SuppressWarnings("unused")
    public MessageHandler discover() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");

                TemplateNode startOfTemplate = getNode();
                try {
                    startOfTemplate.discover(args.get("data"));
                    result.put("information", "Started discovery routine(s) for template.");
                } catch (Exception e) {
                	log.error(e.getMessage());

        			if (log.isTraceEnabled()) {
        				e.printStackTrace();
        			}
        			
        			result.put("error", e.getMessage());
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    /**
     * Get child templates of a template.
     * @return array of child templates.
     */
    public MessageHandler getSubTemplates() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                ArrayNode data = result.putArray("data");

                getNode().getHasComponents().stream()
                        .forEach(child -> data.add(child.toJson()));

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }
}
