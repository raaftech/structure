// Copyright © 2008-2016 RAAF Technology bv

package com.raaftech.structure.base;

import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static boolean initialized = false; 
	private static boolean traceMode = false;
	private static boolean unsafeMode = false;
	private static URI schemaNamespace;
	static final String bundlePathProperty = "bundlePath";

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

	public static String getBundlePath() {
		return System.getProperty(bundlePathProperty);
	}

	public static URI getSchemaNamespace() {
		return schemaNamespace;
	}

	public static void setSchemaNamespace(URI schemaNamespace) {
		Configuration.schemaNamespace = schemaNamespace;
	}

	public static void init() {

		if (!isInitialized()) {
			final String propertyFile = "config.properties";

			try {
				Properties p = new Properties();
				Thread currentThread = Thread.currentThread();
				ClassLoader classLoader = currentThread.getContextClassLoader();
				InputStream inputStream = classLoader.getResourceAsStream(propertyFile);

				if (inputStream.available() > 0) {
					p.load(inputStream);

					if (p.containsKey(bundlePathProperty)) {
						log.info("Setting {} to {} from {}", bundlePathProperty, p.getProperty(bundlePathProperty),
								propertyFile);
						System.setProperty(bundlePathProperty, p.getProperty(bundlePathProperty));
					} else {
						log.error("bundlePathProperty is empty, defaulting to 'bundles'");
						System.setProperty(bundlePathProperty, "src/main/resources/bundles");
					}
				}
				
				initialized = true;
				
			} catch (Exception e) {
				log.error(e.getMessage());

				if (Configuration.isTraceModeOn()) {
					e.printStackTrace();
				}
			}
		}
	}
}
