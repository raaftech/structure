package com.raaftech.structure.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseActor extends GenericActor {

    private Logger log = LoggerFactory.getLogger(BaseActor.class);

    // The Vert.x Verticle start method.
    @Override
    public void start() {
        log.debug("Setting up {} as a message handler", getActorName());
        vertx.eventBus().consumer(getActorName(), this::handleMessage);


    }

    // Get the name of the actor (used with registerHandler).
    protected String getActorName() {
        return this.getClass().getName().toLowerCase();
    }

}
