package com.raaftech.structure.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raaftech.structure.base.GenericActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Boy van Duuren &lt;boy@raaftech.com&gt;.
 */
public class JsonHelper {
    private final static ObjectMapper mapper =  new ObjectMapper();
    private final static Logger log = LoggerFactory.getLogger(JsonHelper.class);

    // A json file loader (looks in your classpath/resources.)
    public static JsonNode jsonFromResource(String resource) {
        JsonNode result;
        InputStream inputStream = JsonHelper.class.getClassLoader().getResourceAsStream(resource);

        try {
            result = mapper.readTree(inputStream);
        } catch (IOException x) {
            log.error(x.getMessage());
            return null;
        }

        return result;
    }
}
