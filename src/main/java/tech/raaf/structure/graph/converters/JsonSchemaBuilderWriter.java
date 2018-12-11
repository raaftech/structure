package tech.raaf.structure.graph.converters;

import org.springframework.core.convert.converter.Converter;
import tech.raaf.structure.helper.JsonSchemaBuilder;

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
