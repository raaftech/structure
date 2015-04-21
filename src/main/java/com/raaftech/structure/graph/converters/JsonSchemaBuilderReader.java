package com.raaftech.structure.graph.converters;

import java.io.IOException;

import com.raaftech.structure.helper.JsonSchemaBuilder;
import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

/**
 * ConstraintReader converts from (JSON) String to JsonSchemaBuilder
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-08
 */
public class JsonSchemaBuilderReader implements Converter<String, JsonSchemaBuilder> {

    @Override
    public JsonSchemaBuilder convert(String s) {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchemaBuilder constraints = null;
        try {
            constraints = new JsonSchemaBuilder((ObjectNode)mapper.readTree(s));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (ProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return constraints;
    }
}
