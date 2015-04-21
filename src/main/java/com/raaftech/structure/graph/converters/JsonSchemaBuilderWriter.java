package com.raaftech.structure.graph.converters;

import com.raaftech.structure.helper.JsonSchemaBuilder;
import org.springframework.core.convert.converter.Converter;

/**
 * ConstraintReader converts from HashMap<String, Constraint> to (JSON) String
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-05
 */
public class JsonSchemaBuilderWriter implements Converter<JsonSchemaBuilder, String> {

    @Override
    public String convert(JsonSchemaBuilder constraints) {
        return constraints.getAsObjectNode().toString();
    }
}
