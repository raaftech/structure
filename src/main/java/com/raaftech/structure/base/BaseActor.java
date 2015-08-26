package com.raaftech.structure.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;

public abstract class BaseActor extends GenericActor {

    private Logger log = LoggerFactory.getLogger(BaseActor.class);

    // The Vert.x Verticle start method.
    @Override
    public void start(Future<Void> startFuture) {
        vertx.eventBus().consumer(getActorName(), this::handleMessage);

        // Tell the console we're up and running.
        log.debug(getActorName() + " ready to serve");
    }

    // Get the name of the actor (used with registerHandler).
    protected String getActorName() {
        return this.getClass().getName().toLowerCase().replace("actor.", "");
    }

}
