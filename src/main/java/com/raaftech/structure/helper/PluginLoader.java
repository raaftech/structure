package com.raaftech.structure.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raaftech.structure.base.Context;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class PluginLoader {

    private Logger log = LoggerFactory.getLogger(PluginLoader.class);

    // We'll usually use the vertx in our context
    // but we'll want to be able to set it to something else for testing
    Vertx vertx;

    public PluginLoader(Vertx vertx) {
        this.vertx = vertx;
    }

    public void loadSystemPlugins() {
        ClassLoader classLoader = PluginLoader.class.getClassLoader();
        Arrays.asList(
                classLoader.getResource("plugins/checkable"),
                classLoader.getResource("plugins/commandable"),
                classLoader.getResource("plugins/discoverable")
                ).stream()
        .forEach(url -> {
            try {
                loadPlugins(url);
            } catch (IOException e) {
                // FATAL!
                log.error("Couldn't load system plugins. Fatal.");
                System.exit(1);
            }
        });
    }

    public void loadPlugins(URL directory) throws IOException {
        Path pluginPath;
        pluginPath = FileSystems.getDefault().getPath(directory.getPath());

        Files.list(pluginPath)
        .filter(Files::isDirectory)
        .forEach(p -> {
            File properties = new File(p + File.separator + "plugin.properties");
            if (properties.exists()) {
                log.info("Deploying plugin " + p.getFileName());
                deployPlugin(properties);
            } else {
                log.warn("Plugin directory " + p.getFileName() + " does not contain " +
                        "a plugin.properties file. Skipping.");
            }
        });
    }

    private void deployPlugin(File propertiesFile) {
        Properties properties = new Properties();
        // Used to check the address of a plugin
        Pattern pattern = Pattern.compile("^[A-Za-z0-9][A-z0-9-]+[A-Za-z0-9]$");
        Boolean addressMatches;
        // Used to check if the address is free
        Set<String> registeredPlugins = Context.INSTANCE.getRegisteredPlugins();

        if (propertiesFile != null) {
            try {
                properties.load(new FileInputStream(propertiesFile));
            } catch (IOException e) {
                log.error("Error while deploying plugin, skipping. Error was: " + e.getMessage());
            }
        } else {
            log.error("Error while deploying plugin, properties file seems to be null.");
        }

        String path = propertiesFile.getParent();
        String name = properties.getProperty("name");
        String verticle = properties.getProperty("plugin");
        String address = properties.getProperty("address");

        addressMatches = pattern.matcher(address).matches();
        if (!addressMatches) {
            log.error("Error while deploying plugin " + name + ", invalid address.");
            return;
        }

        if (registeredPlugins.contains(address)) {
            log.error("Error while deploying plugin " + name + ", address is in use.");
            return;
        }

        if (name != null && verticle != null) {
            JsonObject metadata = new JsonObject()
                    .put("name", name)
                    .put("address", address)
                    .put("path", path);
            DeploymentOptions options = new DeploymentOptions().setConfig(metadata);
            registeredPlugins.add(address);
            vertx.deployVerticle("plugins" + File.separator +
                    new File(new File(path).getParent()).getName() + File.separator +
                    new File(path).getName() + File.separator + verticle, options, res -> {
                        if (res.failed()) {
                            log.error("Error while deploying plugin due to " + res.cause().getMessage() + ".. unregistering.");
                            registeredPlugins.remove(address);
                        }
                    });
        }
    }
}
