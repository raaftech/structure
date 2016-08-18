package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * BundleLoader is responsible for taking a loader and making sure all the definitions in that bundle are
 * made available to Structure.
 */
public class BundleLoader {

    static final Logger log = LoggerFactory.getLogger(BundleLoader.class);

    /**
     * Load all the packages defined in a bundle definition.
     * @param bundleJson The bundle definition to load.
     */
    public void load(String bundleJson) throws IOException {
        // Start by validating the provided bundle against our schema
        ObjectMapper mapper = new ObjectMapper();
        Bundle bundle = mapper.readValue(bundleJson, Bundle.class);
        log.debug("Successfully deserialized bundle \"" + bundle.getName() + "\" from JSON.");
    }
}
