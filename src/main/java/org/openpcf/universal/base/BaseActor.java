// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.base;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Give a description of BaseActor here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-09-15
 */
public abstract class BaseActor extends Verticle {

    Map<String, MessageHandler> handlers;
    Map<String, Loggable> loggers;

    // Constructor.
    protected BaseActor() {
        // Initialize arrays.
        Method methods[] = this.getClass().getMethods();
        List<Method> messageHandlers = new ArrayList<>();

        // Fill messageHandlers.
        for (Method method : methods) {
            if (method.getReturnType().equals(MessageHandler.class)) {
                messageHandlers.add(method);
            }
        }

        // Create the handlers hash map.
        handlers = new HashMap<>();
        for (Method mh : messageHandlers) {
            String s = mh.getName();
            try {
                handlers.put(s, (MessageHandler) mh.invoke(this));
            } catch (IllegalAccessException | InvocationTargetException x) {
                log("error", "Failed to access or invoke method " + s + ": " + x.getMessage());
            }
        }

        // Create the loggers hash map.
        loggers = new HashMap<>();
        loggers.put("info", new Loggable() {
            public void log(String message) {
                container.logger().info(getActorName() + ": " + message);
            }
        });
        loggers.put("warn", new Loggable() {
            public void log(String message) {
                container.logger().warn(getActorName() + ": " + message);
            }
        });
        loggers.put("error", new Loggable() {
            public void log(String message) {
                container.logger().error(getActorName() + ": " + message);
            }
        });
        loggers.put("fatal", new Loggable() {
            public void log(String message) {
                container.logger().fatal(getActorName() + ": " + message);
            }
        });
        loggers.put("debug", new Loggable() {
            public void log(String message) {
                container.logger().debug(getActorName() + ": " + message);
            }
        });
    }

    // Get the name of the actor (used with registerHandler).
    private String getActorName() {
        return this.getClass().getName().toLowerCase();
    }

    // A private interface for the log helper method.
    private interface Loggable {
        public abstract void log(String message);
    }

    // A logger for actors with nice defaults.
    public void log(String level, String message) {
        if (!loggers.containsKey(level)) {
            container.logger().error(getActorName() + ": invalid level " + level + "specified");
            container.logger().warn(getActorName() + ": setting level to \"info\"");
            level = "info";
        }
        loggers.get(level).log(message);
    }

    // The Vert.x Verticle start method.
    public void start() {
        vertx.eventBus().registerHandler(getActorName(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                String msghDefault = "showUsage";
                String msgh;
                JsonArray args;

                // Verify if we got some sane stuff back.
                if (message.body().containsField("msgh")) {
                    msgh = message.body().getString("msgh");
                    if (!handlers.containsKey(msgh)) {
                        log("error", "msgh " + msgh + " unknown");
                        log("warn", "setting msgh to " + msghDefault);
                        msgh = msghDefault;
                    }
                } else {
                    log("error", "msgh missing from message");
                    log("warn", "setting msgh to " + msghDefault);
                    msgh = msghDefault;
                }

                if (message.body().containsField("args")) {
                    args = message.body().getArray("args");
                } else {
                    args = new JsonArray();
                }

                // Route message to message handler and execute.
                message.reply(handlers.get(msgh).exec(args));
            }
        });

        // Tell the console we're up and running.
        log("info", "started");
    }

    // A public interface for MessageHandler objects.
    public interface MessageHandler {
        public String exec(JsonArray args);
    }

    // The default usage  handler.
    public MessageHandler showUsage() {
        return new MessageHandler() {
            public String exec(JsonArray args) {
                String msg = "I am showUsage!";
                log("info", msg);
                return msg;
            }
        };
    }

    // The default version handler.
    public MessageHandler showVersion() {
        return new MessageHandler() {
            public String exec(JsonArray args) {
                String msg = "I am showVersion!";
                log("info", msg);
                return msg;
            }
        };
    }
}
