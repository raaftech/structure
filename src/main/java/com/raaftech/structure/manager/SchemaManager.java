package com.raaftech.structure.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.JsonHelper;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class SchemaManager {

    private static final Logger log = LoggerFactory.getLogger(SchemaManager.class);
    private static final TypeRepository typeRepository = Context.getTypeRepository();

    public void getSchema(RoutingContext routingContext) {
        final String commonSchema = "common.schema.json";
        log.debug("Received schema request on REST API.");
        final String schemaName = routingContext.request().getParam("schema");
        if (schemaName == null) {
            log.error("No schema name given, could not process schema request.");
            routingContext.response().setStatusCode(400).close();
            return;
        }

        final String typeName = schemaName.split("\\.")[0];
        final JsonNode schema;
        if (Objects.equals(commonSchema, schemaName)) {
            log.debug("Schema \"{}\" requested, retrieving it from the resources.");
            schema =  JsonHelper.jsonFromResource("schema/common.schema.json");
        } else {
            log.debug("Schema \"{}\" requested, querying for type \"{}\".", schemaName, typeName);
            Type foundType = typeRepository.findOneByCaseInsensitiveName(typeName);
            if (foundType == null) {
                log.error("Type \"{}\" not found, returning 404.", typeName);
                routingContext.response().setStatusCode(404).end();
                return;
            } else {
                log.debug("Type \"{}\" found, returning its schema.", typeName);
                schema = foundType.getSchema().getAsObjectNode();
            }
        }
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(schema));
    }
}
