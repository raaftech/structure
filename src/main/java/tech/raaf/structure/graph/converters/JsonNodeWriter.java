package tech.raaf.structure.graph.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.convert.converter.Converter;

/**
 * ConstraintReader converts from ObjectNode to (JSON) String
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-05
 */
public class JsonNodeWriter implements Converter<JsonNode, String> {

    @Override
    public String convert(JsonNode json) {
        return json.toString();
    }
}
