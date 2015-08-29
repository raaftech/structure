package com.raaftech.structure.actor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.BaseActor;
import com.raaftech.structure.base.Context;

import io.vertx.core.impl.DeploymentManager;
import io.vertx.core.impl.VertxInternal;

public class BusManager extends BaseActor {

    public MessageHandler list() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();

                DeploymentManager dp =  new DeploymentManager((VertxInternal) Context.INSTANCE.getVertx());

                for (String x : dp.deployments()) {
                    result.put("dp",x);
                }

                for (String id : vertx.deploymentIDs()) {
                    result.put("id",id);
                }
                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/busmanager.list.json");
            }
        };
    }
}
