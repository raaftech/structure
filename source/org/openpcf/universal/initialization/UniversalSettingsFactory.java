/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.initialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Give a description of UniversalSettingsFactory here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-02-25
 * @version 0.6
 */
public final class UniversalSettingsFactory {

    // Make sure this class is non-instantiable by making the constructor private.
    private UniversalSettingsFactory() {
        throw new AssertionError();
    }

    public static Properties create(final Class clazz) {

        Properties settings = new Properties();

        try {
            InputStream input = clazz.getResourceAsStream("/universal.properties");
            settings.load(input);


        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return settings;
    }
}
