// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.actor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openpcf.universal.base.BaseActor;

import java.util.ArrayList;

/**
 * The schema management verticle.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-09-11
 */
public class SchemaManager extends BaseActor {

    // Check if the database is available.
    private Boolean databaseIsAvailable() {
        return false;
    };

    // Check if the database contains data.
    private Boolean databaseIsUsed() {
        return false;
    };

    // Check if database is ours.
    private Boolean databaseIsOurs() {
        return false;
    };

    // Check update subscriptions/location.
    private ArrayList<String> listUpdateSites() {
        return new ArrayList<>();

    }

    // Fetch new schema files from update server.
    private void fetchUpdates() {};

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
    private void databaseBackup() {};

    // Restore backup of current database.
    private void databaseRestore() {};

    // Apply updated schema.
    private void applyUpdates() {};

    // Undo applied update.
    private void undoUpdates() {};


    public MessageHandler reinit() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("says", "I am reinit!");
                return response(request, result, 0);
            }

            public JsonNode help(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("name", "reinit");
                result.put("description", "reinit initializes a clean, empty Neo4j database with OpenPCF schema.");

                return result;
            }
        };
    }

    public MessageHandler update() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("says", "I am update!");
                return response(request, result, 0);
            }

            public JsonNode help(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("name", "update");
                result.put("description", "Updates or creates a Neo4j database with OpenPCF schema.");

                return result;
            }
        };
    }

    public MessageHandler status() {
        return new MessageHandler() {
            public JsonNode exec(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("says", "I am status!");
                return response(request, result, 0);
            }

            public JsonNode help(JsonNode request) {
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("name", "status");
                result.put("description", "Request status.");

                return result;
            }
        };
    }
}
