package tech.raaf.structure.bus.manager;

import io.vertx.core.json.JsonObject;
import java.util.Iterator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.annotations.SchemaPath;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.GenericActor;
import tech.raaf.structure.graph.repositories.InstrumentRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Give a description of InstrumentManager here.
 *
 * @author Boy van Duuren <boy@raaf.tech>
 * @since 2016-06-08
 */

public class InstrumentManager extends GenericActor {

  private static final Logger log = LoggerFactory.getLogger(InstrumentManager.class);
  private final InstrumentRepository instrumentRepository = Context.getInstrumentRepository();

  private void loadInstruments() {
    // TODO: Load the instruments from the bundles again, see BundleLoader
  }

  private void purgeInstruments() {
    log.info("Purging instruments.");
    instrumentRepository.purgeInstruments();
    Context.setInstrumentsLoaded(false);
  }

  public MessageHandler reloadInstruments() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        ObjectNode result = mapper.createObjectNode();

        purgeInstruments();
        // TODO: This needs to be a Vert.x thread
        new Thread() {
          public void run() {
            loadInstruments();
          }
        }.run();

        result.put("data", "Success");
        return response(request, result);
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/instrumentmanager.reloadinstruments.json");
      }
    };
  }

  public MessageHandler removeInstruments() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        ObjectNode result = mapper.createObjectNode();

        purgeInstruments();

        result.put("data", "Success");
        return response(request, result);
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/instrumentmanager.removeinstruments.json");
      }
    };
  }

  /*
   * This MessageHandler is used to dispatch the invocation of a capability on multiple instances.
   * We'll simply get one of the UUIDs, determine the grouping it belongs to, assume all UUIDs that are passed
   * belong to that grouping, create a taskId and then send a message containing the capability, the arguments and
   * the taskId to those instances' "runCapability" handler.
   *
   * IMPORTANT: Take note that we assume all UUIDs belong to the same grouping.
   */
  @SchemaPath("resource:/schema/manager/instrumentmanager/runCapability.schema.json")
  public MessageHandler runCapability() {
    return new MessageHandler() {
      @Override
      public JsonNode exec(JsonNode request) {
        final ObjectNode result = mapper.createObjectNode();

        // This depends on Structure being bootstrapped, return if not
        if (Context.getSelfGroup() == null) {
          result.put("error", "Structure isn't bootstrapped.");
          return response(request, result);
        }

        final String instanceActorFormat = "tech.raaf.structure.bus.node.%s";
        final JsonNode args = request.get("args");
        final ArrayNode uuids = (ArrayNode) args.get("uuids");
        final UUID taskId = UUID.randomUUID();

        // Construct the message to pass to the instances
        final JsonObject message = new JsonObject()
          .put("msgh", "runCapability")
          .put(
            "args",
            new JsonObject().put("capability", args.get("capability").asString()).put("taskId", taskId.toString())
          );
        if (args.has("arguments")) {
          message.getJsonObject("args").put("arguments", new JsonObject(args.get("arguments").toString()));
        }
        if (args.has("timeAllocated")) {
          message.getJsonObject("args").put("timeAllocated", args.get("timeAllocated").asInt());
        }

        // For every UUID send the message to its address on the bus
        for (final Iterator<JsonNode> iter = uuids.values().iterator(); iter.hasNext(); ) {
          final UUID uuid = UUID.fromString(iter.next().asString());
          final String address = String.format(instanceActorFormat, uuid);
          log.info("Sending message to " + address);
          vertx
            .eventBus()
            .request(address, message)
            .onFailure(cause -> log.error("Error while sending message to " + address + ": " + cause.getMessage()));
        }

        result.put("data", taskId.toString());
        return response(request, result);
      }

      @Override
      public JsonNode help(JsonNode request) {
        return jsonFromResource("help/instrumentmanager.runcapability.json");
      }
    };
  }
}
