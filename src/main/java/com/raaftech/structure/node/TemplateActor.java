package com.raaftech.structure.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.NodeActor;
import com.raaftech.structure.graph.Template;
import com.raaftech.structure.graph.repositories.TemplateRepository;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Boy van Duuren &lt;boy@raaftech.com&gt;.
 */

public class TemplateActor extends NodeActor<Template> {

    private Logger log = LoggerFactory.getLogger(TemplateActor.class);

    public TemplateActor(Template node) {
        super(node);
    }

    @Override
    protected String getSecondaryAddress() {
        return null;
    }

    // Next methods are exposed on the eventbus

    /**
     * Remove a Template from a Template, or
     * when the Template this is called on is the start of
     * a Template, remove the whole template.
     * @return A JSON object with an error field if an error occurred.
     */
    // TODO: Undeploy Templates when they are removed from the database
    @SuppressWarnings("unused")
    public MessageHandler removeFromTemplate() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");

                TemplateRepository templateRepository = Context.getTemplateRepository();
                Template node = getNode();
                Template parent = node.getParentComponent();

                Transaction tx = Context.getGraphDatabase().beginTx();
                try {
                    if (parent != null) {
                        String templateUuid = parent.getUuid();
                        if (parent.removeComponent(node)) {
                            parent.save();
                            result.put("data", "Removed node " + uuid + " from Template " + templateUuid);
                        } else {
                            result.put("error", "Didn't remove node " + uuid + " from Template " + templateUuid + ", " +
                                    "rolled back transaction.");
                            tx.failure();
                        }
                    } else {
                        long components = node.removeAllComponents() + 1;
                        templateRepository.delete(node);
                        result.put("data", "Removed Template " + uuid + " and its children.");
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

        			if (Configuration.isTraceModeOn()) {
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
     * Instantiate a template resulting in a new Instance group.
     * @return A JSON object containing the first Instance in the group's uuid,
     * or a JSON object with an error field when an error occurs.
     */
    @SchemaPath("schema/node/template/instantiate.schema.json")
    @SuppressWarnings("unused")
    public MessageHandler instantiate() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                TemplateRepository templateRepository = Context.getTemplateRepository();
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");

                JsonNode instancedata = args.get("instancedata");
                Template template = getNode();

                if (template != null && instancedata != null) {
                    try {
                        String uuid = template.instantiate(instancedata);
                        if (uuid != null) {
                            result.put("data", uuid);
                        } else {
                            result.put("error", "Error while instantiating template.");
                        }
                    } catch (RuntimeException e) {
                    	log.error(e.getMessage());

            			if (Configuration.isTraceModeOn()) {
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
    @SchemaPath("schema/node/template/discover.schema.json")
    @SuppressWarnings("unused")
    public MessageHandler discover() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");

                Template startOfTemplate = getNode();
                try {
                    startOfTemplate.discover(args.get("data"));
                    result.put("information", "Started discovery routine(s) for template.");
                } catch (Exception e) {
                	log.error(e.getMessage());

        			if (Configuration.isTraceModeOn()) {
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
