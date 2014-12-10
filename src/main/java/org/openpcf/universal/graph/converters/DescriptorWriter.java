package org.openpcf.universal.graph.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openpcf.universal.graph.Descriptor;
import org.springframework.core.convert.converter.Converter;

import java.util.HashMap;

/**
 * ConstraintReader converts from HashMap<String, Constraint> to (JSON) String
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-05
 */
public class DescriptorWriter implements Converter<HashMap<String, Descriptor>, String> {

    @Override
    public String convert(HashMap<String, Descriptor> constraints) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonConstraint = null;
        try {
            jsonConstraint = mapper.writeValueAsString(constraints);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return jsonConstraint;
    }
}
