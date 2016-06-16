package com.raaftech.structure.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.GenericActor;
import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.repositories.InstanceNodeRepository;
import com.raaftech.structure.helper.InstrumentLoader;
import com.raaftech.structure.helper.InstrumentRunner;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Give a description of ServiceStatusManager here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2016-06-08
 */

public class InstrumentManager extends GenericActor {

    private final static Logger log = LoggerFactory.getLogger(TaskManager.class);
    private final InstanceNodeRepository instanceNodeRepository = Context.INSTANCE.getInstanceNodeRepository();

    private void loadInstruments() {
        // Load all instruments
        if (System.getProperty("instruments") == null) {
            System.setProperty("instruments", new File("ht").getAbsolutePath());
        }
        String instruments = System.getProperty("instruments");
        Arrays.asList(instruments.split(",")).stream()
                .distinct()
                .forEach(InstrumentLoader::loadHandlers);
        Context.INSTANCE.setInstrumentsLoaded(true);
    }

    private void purgeInstruments() {
        log.info("Purging instruments.");
        instanceNodeRepository.purgeHandlers();
        Context.INSTANCE.setInstrumentsLoaded(false);
        vertx.eventBus().send("com.raaftech.structure.manager.servicestatusmanager",
                new JsonObject("{\"msgh\": \"stopChecking\"}"));
    }

    @SuppressWarnings("unused")
    public MessageHandler reloadInstruments() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();

                purgeInstruments();
                new Thread()
                {
                    public void run() {
                        loadInstruments();
                    }
                }.run();

                result.put("data", "Success");
                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    @SuppressWarnings("unused")
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
                return null;
            }
        };
    }

}
