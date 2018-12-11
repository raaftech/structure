package tech.raaf.structure.bus.manager;

import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.graph.Type;
import tech.raaf.structure.graph.repositories.TypeRepository;

public class SchemaManager {

    private static final Logger log = LoggerFactory.getLogger(SchemaManager.class);
    private static final TypeRepository typeRepository = Context.getTypeRepository();

    public void getSchema(RoutingContext routingContext) {
        log.debug("Received schema request on REST API.");
        final String schemaName = routingContext.request().getParam("schema");
        if (schemaName == null) {
            log.error("No schema name given, could not process schema request.");
            routingContext.response().setStatusCode(400).close();
            return;
        }

        final String typeName = schemaName.split("\\.")[0];
        log.debug("Schema \"{}\" requested, querying for type \"{}\".", schemaName, typeName);
        Type foundType = typeRepository.findOneByCaseInsensitiveName(typeName);
        if (foundType == null) {
            log.error("Type \"{}\" not found, returning 404.", typeName);
            routingContext.response().setStatusCode(404).end();
        } else {
            log.debug("Type \"{}\" found, returning its schema.", typeName);
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(foundType.getSchema().getAsObjectNode()));
        }
    }
}
