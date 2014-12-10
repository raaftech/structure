package org.openpcf.universal.graph.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openpcf.universal.graph.Descriptor;
import org.springframework.core.convert.converter.Converter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * ConstraintReader converts from (JSON) String to HashMap<String, Constraint>
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-08
 */
public class DescriptorReader implements Converter<String, HashMap<String, Descriptor>> {

    @Override
    public HashMap<String, Descriptor> convert(String s) {
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, Descriptor> constraints = new HashMap<>();

        try {
            JsonNode jsonConstraints = mapper.readTree(s);
            Iterator<Map.Entry<String, JsonNode>> itr = jsonConstraints.fields();
            while (itr.hasNext()) {
                Map.Entry<String, JsonNode> node = itr.next();
                String key = node.getKey();
                JsonNode value = node.getValue();
                constraints.put(key, new Descriptor(value.get("kvType").asText(), value.get("required").asBoolean()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return constraints;
    }
}
