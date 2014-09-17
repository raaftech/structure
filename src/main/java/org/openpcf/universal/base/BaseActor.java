// Copyright © 2008-2014 RAAF Technology bv

package org.openpcf.universal.base;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.io.IOException;
import java.io.InputStream;
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

    public boolean isValidJson(String body) {
        try {
            new JsonObject(body);
        } catch (Exception x) {
            return false;
        }
        return true;
    }

    public boolean isAlphaNumeric(String str) {
        return str.matches("[0-9a-zA-Z]+");
    }

    public JsonObject bodyFilter(JsonObject body) {

        String msghDefault = "showUsage";
        String msghRetrieved;
        String msghAlphaNumeric;

        JsonObject argsDefault = new JsonObject("{}");
        String argsRetrieved;

        // Verify if msgh field exists.
        if (body.containsField("msgh")) {
            msghRetrieved = body.getString("msgh");
            // Verify if msgh is a alpha-numeric string.
            if (isAlphaNumeric(msghRetrieved)) {
                msghAlphaNumeric = msghRetrieved;
                // Verify if msgh is an existing handler.
                if (!handlers.containsKey(msghAlphaNumeric)) {
                    // Unknown message handler specified, replace msgh.
                    log("error", "Field \"msgh\" not a valid message handler: " + msghAlphaNumeric);
                    log("warn", "Valid \"msgh\" values are: " + Arrays.asList(handlers.values()));
                    log("warn",  "Setting \"msgh\" to default value: " + msghDefault);
                    body.removeField("msgh");
                    body.putString("msgh", msghDefault);
                }
            } else {
                // Invalid characters in msgh, replace msgh.
                log("error", "Field \"msgh\" contains invalid characters: " + msghRetrieved);
                log("warn",  "Setting \"msgh\" to default value: " + msghDefault);
                body.removeField("msgh");
                body.putString("msgh", msghDefault);
            }
        } else {
            // Non-existing msgh, adding default msgh.
            log("warn", "Field \"msgh\" not in JSON body");
            log("warn",  "Setting \"msgh\" to default value: " + msghDefault);
            body.putString("msgh", msghDefault);
        }

        // Verify if args field exists.
        if (body.containsField("args")) {
            argsRetrieved = body.getValue("args").toString();
            // Verify if args is a json object.
            if (!isValidJson(argsRetrieved)) {
                // Invalid args, replace args.
                log("error", "Field \"args\" not of type JSON object: " + argsRetrieved);
                log("warn",  "Setting \"args\" to empty JSON object: " + argsDefault);
                body.removeField("args");
                body.putObject("args", argsDefault);
            }
        } else {
            // Non-existing args, which is ok, adding default args.
            body.putObject("args", argsDefault);
        }

        return body;
    }

    // The Vert.x Verticle start method.
    public void start() {
        vertx.eventBus().registerHandler(getActorName(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject verifiedMessageBody = bodyFilter(message.body());
                String msgh = verifiedMessageBody.getString("msgh");
                JsonObject args = verifiedMessageBody.getObject("args");

                // Route message to message handler and execute.
                message.reply(handlers.get(msgh).exec(args));
            }
        });

        // Tell the console we're up and running.
        log("info", "started");
    }

    // A public interface for MessageHandler objects.
    public interface MessageHandler {
        public JsonObject exec(JsonObject args);
        public JsonObject help(JsonObject args);
    }

    // The default usage  handler.
    public MessageHandler showUsage() {
        return new MessageHandler() {
            public JsonObject exec(JsonObject args) {
                JsonObject exec = new JsonObject();
                JsonArray result = new JsonArray();
                exec.putArray("result", result);
                for (String msgh : handlers.keySet()) {
                    result.addObject(handlers.get(msgh).help(args));
                }
                return exec;
            }

            public JsonObject help(JsonObject args) {
                JsonObject help = new JsonObject();
                help.putString("name", "showUsage");
                help.putObject("synopsis", new JsonObject("{ \"msgh\": \"showUsage\" }"));
                help.putString("description", "showUsage shows a cumulative help message from all message handlers in this actor.");
                return help;
            }
        };
    }

    // The default version handler.
    public MessageHandler showVersion() {
        return new MessageHandler() {
            public JsonObject exec(JsonObject args) {
                JsonObject exec = new JsonObject();
                JsonObject result = new JsonObject();
                exec.putObject("result", result);

                Properties properties = new Properties();
                String release = "release.properties";

                InputStream inputStream = getClass().getClassLoader().getResourceAsStream(release);
                try {
                    properties.load(inputStream);
                } catch (IOException x) {
                    log("error", x.getMessage());
                }

                if (properties.containsKey("release.version")) {
                        result.putString("version", properties.getProperty("release.version").replaceAll("[\'\"]", ""));
                };

                return exec;
            }

            public JsonObject help(JsonObject args) {
                JsonObject help = new JsonObject();
                help.putString("name", "showVersion");
                help.putObject("synopsis", new JsonObject("{ \"msgh\": \"showVersion\" }"));
                help.putString("description", "showVersion returns the version of the actor or framework.");
                return help;
            }
        };
    }
}
