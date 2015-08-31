package com.raaftech.structure.worker;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;


public class ScanWorker extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(ScanWorker.class);

    @Override
    public void start() {
        log.debug("Listen for {} on addr: {}", this.getClass().getSimpleName(), "com.raaftech.structure.worker.scanworker");
        vertx.eventBus().consumer("com.raaftech.structure.worker.scanworker", handler -> {
            JsonObject body = (JsonObject)handler.body();
            try {
                Socket sock = new Socket();
                String ipAddress = body.getJsonObject("args").getString("address");
                sock.connect(new InetSocketAddress(ipAddress, 22), 200);
                if (sock.isConnected()) { System.out.println("Found live ip " + ipAddress); }
                sock.close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
            }
        });
    }
}
