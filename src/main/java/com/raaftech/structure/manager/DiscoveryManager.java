package com.raaftech.structure.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.GenericActor;
import com.raaftech.structure.graph.repositories.ComponentRepository;
import com.raaftech.structure.graph.repositories.TemplateRepository;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DiscoveryManager extends GenericActor {

    private static final ComponentRepository componentRepository = Context.getComponentRepository();
    private static final TemplateRepository templateRepository = Context.getTemplateRepository();
    private static final Logger log = LoggerFactory.getLogger(GenericActor.class);

    public MessageHandler instantiateDiscoveredComputer() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                JsonNode args = request.get("args");
                ArrayNode instanceData = (ArrayNode) args.get("instancedata");
                final String dnsNameTemplate = "94d10031-5246-4233-b25d-b5f8ef9d0b41";
                final String template = "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa";
                Iterable<JsonNode> iterable = () -> instanceData.elements();
                Stream<JsonNode> instances = StreamSupport.stream(iterable.spliterator(), false);

                instances
                        .filter(node -> node.get("templateUuid").asText().equals(dnsNameTemplate))
                        .map(node -> node.get("data").get("instances"))
                        .filter(data -> data.size() > 0)
                        .forEach(arr -> {
                            Iterator<JsonNode> elements = arr.elements();
                            boolean found = false;
                            String dnsname = "";
                            while (elements.hasNext() && !found) {
                                JsonNode el = elements.next();
                                dnsname = el.get("dnsname").asText();
                                found = componentRepository.findHostname(dnsname) != null;
                                if (found) {
                                    log.info("Computer with hostname {} already exists", dnsname);
                                }
                            }

                            if (!found) {
                                log.info("Creating new computer with hostname {} ", dnsname);
                                final String newComputerUuid = templateRepository.findByUuid(template).instantiate(instanceData);
                                // send out message on status channel if this is the start of a component group
                                ObjectNode data = mapper.createObjectNode()
                                        .put("uuid", newComputerUuid);
                                log.debug("Sending status message to status channel");
                                try {
                                    log.debug(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data.toString()));
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                                Context.getVertx().eventBus().publish("com.raaftech.structure.status",
                                        new JsonObject("{\"source\":\"" + this.getClass().getSimpleName() + "\", "
                                                + "\"event\": \"discoveredComputer\", \"data\": " + new JsonObject(data.toString()) + "}"));
                            }
                        });

                return response(request, mapper.createObjectNode());
            };

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }
}
