package com.raaftech.structure.actor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.BaseActor;

import io.vertx.core.json.JsonObject;

public class DiscoveryManager extends BaseActor {

    private Logger log = LoggerFactory.getLogger(DiscoveryManager.class);

    public MessageHandler scansubnet() {
        return new MessageHandler() {

            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                JsonNode args = request.get("args");
                JsonNode schema = jsonFromResource("schema/discoverymanager/scansubnet.schema.json");
                if (isValidJson(schema, args)) {
                    SubnetUtils utils = new SubnetUtils(args.get("subnet").asText());
                    utils.setInclusiveHostCount(false);
                    Arrays.stream(utils.getInfo().getAllAddresses()).forEach(addr -> {
                        if (isPrivateAddress(addr)) {
                            JsonObject message = new JsonObject(
                                    "{ \"msgh\" : \"scanhostlive\", \"args\" :" +
                                            "  { \"address\" : \"" + addr + "\" } }");

                            vertx.eventBus().send("com.raaftech.structure.actor.worker.scanworker", message);
                        } else {
                            log.warn("Didn't scan " + addr + ", because it's not a site local address.");
                        }
                    });

                    result.put("information", "Started scanning " + utils.getInfo().getAddressCount() + " addresses.");
                } else {
                    result.put("error", "JSON didn't pass validation.");
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

    private boolean isPrivateAddress(String address) {
        try {
            return InetAddress.getByName(address).isSiteLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
