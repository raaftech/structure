// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.actor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.AsyncResult;
import io.vertx.core.AsyncResultHandler;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.openpcf.universal.base.BaseActor;
import org.openpcf.universal.base.Context;

import java.util.ArrayList;
import java.util.UUID;

/**
 * The SchemaManager actor.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-09-11
 */
public class SchemaManager extends BaseActor {

    // Check if the database is available.
    private Boolean databaseIsAvailable() {
        return false;
    }

    // Check if the database contains data.
    private Boolean databaseIsUsed() {
        return false;
    }

    // Check if database is ours.
    private Boolean databaseIsOurs() {
        return false;
    }

    // Check update subscriptions/location.
    private ArrayList<String> listUpdateSites() {
        return new ArrayList<>();
    }

    // Fetch new schema files from update server.
    private void fetchUpdates() {
    }

    // Check current structure schema version.
    private String getCurrentVersion() {
        return "getCurrentVersion stub";
    }

    // Check structure software instance version.
    private String getAvailableVersion() {
        return "getAvailableVersion stub";
    }

    // Check if currentVer and availableVer are different.
    private Boolean currentVersionIsLatest() {
        return false;
    }

    // Create backup of current database.
    private void databaseBackup() {
    }

    // Restore backup of current database.
    private void databaseRestore() {
    }

    // Apply updated schema.
    private void applyUpdates() {
    }

    // Undo applied update.
    private void undoUpdates() {
    }

    public MessageHandler reinit() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("says", "I am reinit!");
                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/schemamanager.reinit.json");
            }
        };
    }

    public MessageHandler update() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                Vertx vertx = Context.INSTANCE.getVertx();

                ObjectNode result = JsonNodeFactory.instance.objectNode();
                String id = UUID.randomUUID().toString();
                result.put("says", id);
                // Deploy a worker verticle that runs the update schema process.
                vertx.deployVerticle("org.openpcf.universal.worker.SchemaManagerUpdate",
                        new DeploymentOptions()
                                .setWorker(true)
                                .setConfig(new JsonObject().put("id", id)),
                        new AsyncResultHandler<String>() {
                            @Override
                            public void handle(AsyncResult<String> stringAsyncResult) {
                                if (stringAsyncResult.succeeded()) {
                                    // TODO: log result
                                    System.out.println("Deployed worker.");
                                } else {
                                    throw new RuntimeException(stringAsyncResult.cause());
                                }
                            }
                        });
                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/schemamanager.update.json");
            }
        };
    }

    public MessageHandler status() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("says", "I am status!");
                return response(request, result);
            }

            public JsonNode help(JsonNode request) {
                return jsonFromResource("help/schemamanager.status.json");
            }
        };
    }
}
