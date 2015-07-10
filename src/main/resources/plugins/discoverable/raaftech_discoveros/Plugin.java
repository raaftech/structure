import org.codehaus.jackson.node.ObjectNode;

import com.raaftech.structure.helper.HandlerPlugin;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

public class Plugin extends HandlerPlugin {

    public void implementation() {
        JsonObject instanceData = new JsonObject()
                .put("name", "discoveredOS")
                .put("desc", "Discovered OS woohoo.")
                .put("ver", "1")
                .put("shortName", "discoveredOS")
                .put("longName", "Discovered OS yo.");
        JsonArray instances = new JsonArray();
        instances.add(instanceData);
        pluginResult.put("data", new JsonObject().put("instances", instances));
        pluginResult.put("uuid", args.get("node").asText());
    }
}