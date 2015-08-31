package com.raaftech.structure.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.graph.Node;
import com.raaftech.structure.graph.repositories.NodeRepository;

import io.vertx.core.eventbus.MessageConsumer;

public abstract class NodeActor<T extends Node> extends BaseActor {

    protected final String startOfAddress = "com.raaftech.structure.node.";

    // We keep a reference to the consumers in case we
    // need to reconfigure or close them later
    private MessageConsumer<Object> primaryConsumer;
    private MessageConsumer<Object> secondaryConsumer;

    private NodeRepository nodeRepository = Context.INSTANCE.getNodeRepository();

    protected String uuid;
    protected String primaryConsumerAddress;

    public NodeActor(T node) {
        this.uuid = node.getUuid();
        primaryConsumerAddress = startOfAddress + getActorName(node);
    }

    // The Vert.x Verticle start method.
    @Override
    public void start() {
        primaryConsumer = vertx.eventBus().consumer(primaryConsumerAddress, this::handleMessage);
        if (getSecondaryAddress() != null) {
            secondaryConsumer = vertx.eventBus().consumer(startOfAddress + getSecondaryAddress(), this::handleMessage);
        }
    }

    protected T getNode() {
        T node = (T) nodeRepository.findByUuid(uuid);
        return node;
    }

    // Used to register the handler address
    protected String getActorName(T node) {
        if (node != null) {
            String[] fullClassName = node.getClass().getName().toString().split("\\.");
            String className = fullClassName[fullClassName.length - 1];
            className = className.replace("Node", "");
            StringBuilder address = new StringBuilder(className.toLowerCase());
            address.append("." + uuid);

            return address.toString();
        } else {
            return null;
        }
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

}