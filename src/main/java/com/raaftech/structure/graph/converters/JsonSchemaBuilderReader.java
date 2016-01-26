package com.raaftech.structure.graph.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.raaftech.structure.helper.JsonSchemaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

import java.io.IOException;

/**
 * ConstraintReader converts from (JSON) String to JsonSchemaBuilder
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-08
 */
public class JsonSchemaBuilderReader implements Converter<String, JsonSchemaBuilder> {

    private Logger log = LoggerFactory.getLogger(JsonSchemaBuilderReader.class);

    @Override
    public JsonSchemaBuilder convert(String s) {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchemaBuilder constraints = null;
        try {
            constraints = new JsonSchemaBuilder((ObjectNode)mapper.readTree(s));
        } catch (ProcessingException | IOException e) {
            log.error(e.getMessage());
        }
        return constraints;
    }
}
