package com.raaftech.structure.helper;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import com.raaftech.structure.base.Context;

public class PluginLoader {

    // We'll usually use the vertx in our context
    // but we'll want to be able to set it to something else for testing
    Vertx vertx;

    public PluginLoader(Vertx vertx) {
        this.vertx = vertx;
    }

    public void loadPlugins() throws IOException {
        ClassLoader classLoader = PluginLoader.class.getClassLoader();
        Path pluginPath;
        URL res = classLoader.getResource("plugins");
        if (res != null) {
            pluginPath = FileSystems.getDefault().getPath(res.getPath());
        } else {
            throw new IOException("Can't open plugins directory.");
        }

        Files.list(pluginPath)
            .filter(Files::isDirectory)
            .forEach(p -> {
                File properties = new File(p + File.separator + "plugin.properties");
                if (properties.exists()) {
                    System.out.println("Deploying plugin " + p.getFileName());
                    deployPlugin(properties);
                } else {
                    System.out.println("Plugin directory " + p.getFileName() + " does not contain " +
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
                e.printStackTrace();
                System.out.println("Error while deploying plugin, skipping.");
            }
        } else {
                System.out.println("Error while deploying plugin, skipping.");
        }

        String path = propertiesFile.getParent();
        String name = properties.getProperty("name");
        String verticle = properties.getProperty("plugin");
        String address = properties.getProperty("address");

        addressMatches = pattern.matcher(address).matches();
        if (!addressMatches) {
            System.out.println("Error while deploying plugin " + name + ", invalid address.");
            return;
        }

        if (registeredPlugins.contains(address)) {
            System.out.println("Error while deploying plugin " + name + ", address is in use.");
            return;
        }

        if (name != null && verticle != null) {
            JsonObject metadata = new JsonObject()
                    .put("name", name)
                    .put("address", address)
                    .put("path", path);
            DeploymentOptions options = new DeploymentOptions().setConfig(metadata);
            registeredPlugins.add(address);
            vertx.deployVerticle("plugins" + File.separator + new File(path).getName() + File.separator + verticle, options, res -> {
                if (res.failed()) {
                    System.out.println("Error while deploying plugin, unregistering.");
                    res.cause().printStackTrace();
                    registeredPlugins.remove(address);
                }
            });
        }
    }
}
