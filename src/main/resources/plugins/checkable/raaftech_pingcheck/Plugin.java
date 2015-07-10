import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.raaftech.structure.helper.HandlerPlugin;

public class Plugin extends HandlerPlugin {

    public void implementation() {
        String node = args.get("node").asText();
        String task = args.get("task").asText();
        JsonObject msg = new JsonObject()
        .put("msgh", "instancenode")
        .put("args", new JsonObject().put("node", node));
        vertx.eventBus().send("com.raaftech.structure.actor.framehelper", msg,
                handler -> {
                    try {
                        JsonNode reply = convertFromJsonObject((JsonObject)handler.result().body());
                        JsonNode result = reply.get("result");
                        String ipAddress = result.get("kv").get("addr").asText();

                        // TODO: Check for safety!
                        // TODO: Make multiplatform
                        Process p = Runtime.getRuntime().exec(new String[] { "ping", "-c1", ipAddress });
                        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String line;
                        p.waitFor();
                        String output = new String();
                        while ((line = reader.readLine()) != null) {
                            if (output.length() == 0) {
                                output = output.concat(line);
                            } else {
                                output = output.concat("\n" + line);
                            }
                        }
                        createCommandResult(node, task, output);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
    }
}