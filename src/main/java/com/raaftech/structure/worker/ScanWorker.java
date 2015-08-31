package com.raaftech.structure.worker;

import java.net.InetSocketAddress;
import java.net.Socket;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;


public class ScanWorker extends AbstractVerticle {

    @Override
    public void start() {
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
