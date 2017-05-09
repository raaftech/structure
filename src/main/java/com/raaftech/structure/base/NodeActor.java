package com.raaftech.structure.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.graph.Node;
import com.raaftech.structure.graph.repositories.NodeRepository;
import io.vertx.core.eventbus.MessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NodeActor<T extends Node> extends GenericActor {

    protected final String startOfAddress = "com.raaftech.structure.node.";
    public static final String SHAREDMAP_ID = "nodeActors";

    // We keep a reference to the consumers in case we
    // need to reconfigure or close them later
    @SuppressWarnings("unused")
    private MessageConsumer<Object> primaryConsumer;
    @SuppressWarnings("unused")
    private MessageConsumer<Object> secondaryConsumer;

    private NodeRepository nodeRepository = Context.getNodeRepository();

    protected String uuid;
    protected String primaryConsumerAddress;

    public NodeActor(T node) {
        this.uuid = node.getUuid();
        primaryConsumerAddress = startOfAddress + uuid;
    }

    private Logger log = LoggerFactory.getLogger(NodeActor.class);

    @Override
    public void start() {
        primaryConsumer = vertx.eventBus().consumer(primaryConsumerAddress, this::handleMessage);
        vertx.sharedData().getLocalMap(SHAREDMAP_ID).put(primaryConsumerAddress, deploymentID());
        log.debug("Listen for {} on addr: {}", this.getClass().getSimpleName(), primaryConsumerAddress);
        if (getSecondaryAddress() != null) {
            String secondaryConsumerAddress = startOfAddress + getSecondaryAddress();
            secondaryConsumer = vertx.eventBus().consumer(secondaryConsumerAddress, this::handleMessage);
            log.debug("Listen for {} on addr: {}", this.getClass().getSimpleName(), secondaryConsumerAddress);
        }
    }

    protected T getNode() {
        return (T) nodeRepository.findByUuid(uuid);
    }

    // Used to register an optional second handler address
    protected abstract String getSecondaryAddress();

    // Default methods every NodeActor exposes on the eventbus
    public MessageHandler uuid() {
        return new MessageHandler() {

            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                result.put("data", uuid);
                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                // TODO Auto-generated method stub
                return null;
            }

        };
    }

    public MessageHandler undeploy() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                vertx.undeploy(deploymentID(), (result) -> {
                    if (result.succeeded()) {
                        log.info("Undeployed NodeActor \"" + primaryConsumerAddress + "\"");
                    } else {
                        log.error("Error while undeploying \"" + primaryConsumerAddress + "\": " + result.cause().getMessage());
                    }
                });
                return response(request, mapper.createObjectNode().put("data", deploymentID()));
            };

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

}