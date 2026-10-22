// Copyright © 2008-2016 RAAF Technology bv

package tech.raaf.structure.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.helper.ConfigurationProxy;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Configuration contains application wide configuration options for
 * structure.
 *
 * @author Freek Alleman <freek.alleman@raaftech.com>
 * @since 2016-09-30
 */
public enum Configuration {
    INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(Configuration.class);
    private static final String BUNDLESEPARATOR = ",";
    private static final Path fallbackBundlePath = Paths.get("src/main/resources/bundles");

    private static boolean initialized = false; 
    private static boolean traceMode = false;
    private static boolean unsafeMode = false;
    private static String webHost;
    private static int webPort;
    private static URI schemaNamespace;
    private static Set<Path> bundlePaths = new HashSet<>();
    private static ConfigurationProxy configurationProxy;
    static final String bundlePathProperty = "bundles";
    // The worker pool of the actors that use the graph
    public static final String ACTOR_POOL = "structure-actors";

    public static boolean isTraceModeOn() {
        return traceMode;
    }

    public static void setTraceMode(boolean b) {
        traceMode = b;
    }

    public static boolean isUnsafeModeOn() {
        return unsafeMode;
    }

    public static void setUnsafeMode(boolean b) {
        unsafeMode = b;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static URI getSchemaNamespace() {
        return schemaNamespace;
    }

    public static void setSchemaNamespace(URI schemaNamespace) {
        Configuration.schemaNamespace = schemaNamespace;
    }

    public static String getWebHost() {
        return webHost;
    }

    public static void setWebHost(String webHost) {
        Configuration.webHost = webHost;
    }

    public static int getWebPort() {
        return webPort;
    }

    public static void setWebPort(int webPort) {
        Configuration.webPort = webPort;
    }

    public static Set<Path> getBundlePaths() {
        return bundlePaths;
    }

    public static ConfigurationProxy getConfigurationProxy() {
        return configurationProxy;
    }

    public static void init() {
        bundlePaths.add(fallbackBundlePath);
    }

    public static void init(ConfigurationProxy configurationProxy) {
        Configuration.configurationProxy = configurationProxy;
        if (!isInitialized()) {
            Optional<String> bundles = configurationProxy.getConfiguration(bundlePathProperty);
            if (bundles.isPresent()) {
                log.info("Setting {} to \"{}\"", bundlePathProperty, bundles.get().trim());
                bundlePaths.addAll(Stream.of(bundles.get().split(BUNDLESEPARATOR))
                        .map(Paths::get)
                        .collect(Collectors.toList()));
            } else {
                log.info("Setting {} to \"{}\"", bundlePathProperty, fallbackBundlePath);
                log.info("Setting {} to \"{}\"", bundlePathProperty, fallbackBundlePath);
                bundlePaths.add(fallbackBundlePath);
            }
        }
        initialized = true;
    }
}
