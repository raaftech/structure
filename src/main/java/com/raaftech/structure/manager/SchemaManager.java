// Copyright © 2008-2014 RAAF Technology bv

package com.raaftech.structure.manager;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.BaseActor;
import com.raaftech.structure.helper.SchemaManagerUpdate;

/**
 * The SchemaManager actor.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-09-11
 */
public class SchemaManager extends BaseActor {

    private Logger log = LoggerFactory.getLogger(SchemaManager.class);

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
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                SchemaManagerUpdate schemaManagerUpdate = new SchemaManagerUpdate();
                try {
                    schemaManagerUpdate.applySchemaUpdates();
                    result.put("says", "success!");
                } catch (RuntimeException e) {
                    log.error(e.getMessage());
                    result.put("error", e.getMessage());
                }
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
