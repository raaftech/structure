package com.raaftech.structure.base;

import io.vertx.core.Future;

public abstract class BaseActor extends GenericActor {

    // The Vert.x Verticle start method.
    @Override
    public void start(Future<Void> startFuture) {
        vertx.eventBus().consumer(getActorName(), this::handleMessage);

        // Tell the console we're up and running.
        log("info", getActorName() + " ready to serve");
    }

    // Get the name of the actor (used with registerHandler).
    protected String getActorName() {
        return this.getClass().getName().toLowerCase().replace("actor.", "");
    }

}
