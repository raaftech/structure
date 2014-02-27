package org.openpcf.universal.initialization;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * A preliminary Universal Settings Singleton.
 *
 * @author Freek Alleman <freek.alleman@raaftech.com>
 * @version 0.1
 * @since 2014-02-25
 */
public enum UniversalSettingsSingleton {
    INSTANCE;

    private Properties settings;

    private UniversalSettingsSingleton() {

        settings = new Properties();

        try {
            InputStream input = UniversalSettingsSingleton.class.getResourceAsStream("/universal.properties");
            settings.load(input);

            // Set universal.home if not set in property file.
            if (!settings.containsKey("universal.home")) {
                settings.setProperty("universal.home", System.getProperty("user.dir"));
            } else {
                Path path = Paths.get(settings.getProperty("universal.home"));
                if (path.isAbsolute()) {
                    settings.setProperty("universal.home", path.toString());
                } else {
                    settings.setProperty("universal.home", System.getProperty("user.dir") + File.separator + path.toString());
                }
            }


            // Set universal.graph.dbtype if not set in property file.
            if (!settings.containsKey("universal.graph.dbtype")) {
                settings.setProperty("universal.graph.dbtype", "titan");
            }


            // Set universal.graph.dbhome if not set in property file.
            if (!settings.containsKey("universal.graph.dbhome")) {
                settings.setProperty("universal.graph.dbhome", settings.getProperty("universal.home") + File.separator + "db");
            } else {
                Path path = Paths.get(settings.getProperty("universal.graph.dbhome"));
                if (path.isAbsolute()) {
                    settings.setProperty("universal.graph.dbhome", path.toString());
                } else {
                    settings.setProperty("universal.graph.dbhome", settings.getProperty("universal.home") + File.separator + path.toString());
                }
            }


            // Set universal.graph.dbindex if not set in property file.
            if (!settings.containsKey("universal.graph.dbindex")) {
                settings.setProperty("universal.graph.dbindex", "es");
            }


        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public Properties getSettings() {
        return settings;
    }
}
