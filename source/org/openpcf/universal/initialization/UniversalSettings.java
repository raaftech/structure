package org.openpcf.universal.initialization;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * A preliminary Universal Settings Singleton.
 *
 * @author Freek Alleman <freek.alleman@raaftech.com>
 * @version 0.1
 * @since 2014-02-25
 */
public enum UniversalSettings {
    INSTANCE;

    private Properties settings;

    private UniversalSettings(){

        settings = new Properties();

        try {
            InputStream input = UniversalSettings.class.getResourceAsStream("/universal.properties");
            settings.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public Properties getSettings(){
        return settings;
    }
}
