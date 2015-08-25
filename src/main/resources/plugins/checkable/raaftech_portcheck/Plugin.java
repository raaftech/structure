import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.ConnectException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.raaftech.structure.base.Application;
import com.raaftech.structure.helper.HandlerPlugin;

public class Plugin extends HandlerPlugin {

    final static Logger log = LoggerFactory.getLogger(Plugin.class);

    public void implementation() {
        String node = args.get("node").asText();
        String task = args.get("task").asText();
        JsonObject msg = new JsonObject()
        .put("msgh", "findOne")
        .put("args", new JsonObject().put("node", node));
        vertx.eventBus().send("com.raaftech.structure.repository.instancenodes", msg,
                handler -> {
                    try {
                        JsonNode reply = convertFromJsonObject((JsonObject)handler.result().body());
                        JsonNode result = reply.get("result").get("data");
                        String ipAddress = result.get("kv").get("addr").asText();
                        int port = result.get("kv").get("port").asInt();

                        try {
                            Socket sock = new Socket();
                            sock.connect(new InetSocketAddress(ipAddress, port), 3000);
                            createDerivedStatusResult(node, task, true);
                            sock.close();
                        } catch (ConnectException e) {
                            createDerivedStatusResult(node, task, false);
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                });
    }
}