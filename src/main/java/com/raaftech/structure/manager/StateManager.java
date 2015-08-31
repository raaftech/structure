package com.raaftech.structure.manager;

import java.util.Iterator;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.GenericActor;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;

/**
 * The StateManager actor.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-10-15
 */
public class StateManager extends GenericActor {

    private InstanceNodeRepository instanceNodeRepository = Context.INSTANCE.getInstanceNodeRepository();

    private Logger log = LoggerFactory.getLogger(StateManager.class);

    public void printAll(JsonNode node) {
        Iterator<String> fieldNames = node.fieldNames();
        while(fieldNames.hasNext()){
            String fieldName = fieldNames.next();
            JsonNode fieldValue = node.get(fieldName);
            if (fieldValue.isObject()) {
                System.out.println(fieldName + " :");
                printAll(fieldValue);
            } else {
                String value = fieldValue.asText();
                System.out.println(fieldName + " : " + value);
            }
        }
    }

    public MessageHandler check() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");
                String name = getArgs(args, "name", "Default");

                result.put("cypher", cypherQuery("MATCH (p:Instance)-[r:HAS*]->(c) WHERE p.name = '" + name + "' RETURN p,r,c"));

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/statemanager.check.json");
            }
        };
    }

    public MessageHandler createcommandresult() {
        return new MessageHandler() {

            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");
                JsonNode schema = jsonFromResource("schema/statemanager/createcommandresult.schema.json");
                if (isValidJson(schema, args)) {
                    String uuid = args.get("node").asText();
                    String taskId = args.get("task").asText();
                    String commandResult = args.get("commandresult").asText();
                    Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
                    try {
                        InstanceNode instanceNode = instanceNodeRepository.findByUuid(uuid);

                        if (instanceNode == null) {
                            throw new RuntimeException("Unable to find InstanceNode with uuid " + uuid + ".");
                        }

                        ObjectMapper mapper = new ObjectMapper();
                        ObjectNode obj = mapper.createObjectNode()
                                .put("name", "CommandResult")
                                .put("desc", "Result of an executed command.")
                                .put("task", taskId)
                                .put("data", commandResult)
                                .put("dateCreated", System.currentTimeMillis());

                        InstanceNode resultNode = new InstanceNode(obj, "6991d878-52be-467d-893a-6b95074f04c2");
                        instanceNode.addResult(resultNode);
                        resultNode.save();

                        result.put("says", "Created result " + resultNode.getUuid() + ".");

                        tx.success();
                    } catch (RuntimeException e) {
                        tx.failure();
                        log.error(e.getMessage());
                        result.put("error", e.getMessage());
                    } finally {
                        tx.close();
                    }
                } else {
                    result.put("error", "Message didn't validate against method schema.");
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    public MessageHandler createderivedstatusresult() {
        return new MessageHandler() {

            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");
                JsonNode schema = jsonFromResource("schema/statemanager/createderivedstatusresult.schema.json");
                if (isValidJson(schema, args)) {
                    String uuid = args.get("node").asText();
                    String taskId = args.get("task").asText();
                    boolean commandResult = args.get("status").asBoolean();
                    Transaction tx = Context.INSTANCE.getGraphDatabase().beginTx();
                    try {
                        InstanceNode instanceNode = instanceNodeRepository.findByUuid(uuid);

                        if (instanceNode == null) {
                            throw new RuntimeException("Unable to find InstanceNode with uuid " + uuid + ".");
                        }

                        ObjectMapper mapper = new ObjectMapper();
                        ObjectNode obj = mapper.createObjectNode()
                                .put("name", "DerivedStatusResult")
                                .put("desc", "A result that represents a status.")
                                .put("task", taskId)
                                .put("status", commandResult)
                                .put("dateCreated", System.currentTimeMillis());

                        InstanceNode resultNode = new InstanceNode(obj, "9565a2ca-29e4-4c36-8f17-97e9a0074ded");
                        instanceNode.addResult(resultNode);
                        resultNode.save();

                        result.put("says", "Created result " + resultNode.getUuid() + ".");

                        tx.success();
                    } catch (RuntimeException e) {
                        tx.failure();
                        log.error(e.getMessage());
                        result.put("error", e.getMessage());
                    } finally {
                        tx.close();
                    }
                } else {
                    result.put("error", "Message didn't validate against method schema.");
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }
}
