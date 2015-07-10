import org.codehaus.jackson.node.ObjectNode;

import com.raaftech.structure.helper.HandlerPlugin;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

public class Plugin extends HandlerPlugin {

    public void implementation() {
        String ipAddress = args.get("ip").asText();
        JsonObject instanceData = new JsonObject()
                .put("name", "eth0")
                .put("desc", "Discovered eth0. (because found IP " + ipAddress + ".")
                .put("rev", "1");
        JsonArray instances = new JsonArray();
        instances.add(instanceData);
        pluginResult.put("data", new JsonObject().put("instances", instances));
        pluginResult.put("uuid", args.get("node").asText());
    }
}