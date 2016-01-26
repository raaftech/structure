package com.raaftech.structure.graph.converters;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.convert.converter.Converter;

import java.util.LinkedHashMap;

/**
 * Give a description of LinkedHashMapWriter here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-01-26
 */
public class LinkedHashMapWriter implements Converter<LinkedHashMap<String, String>, String>  {

    @Override
    public String convert(LinkedHashMap<String, String> linkedHashMap) {
        ObjectNode jsonLinkedHashMap = JsonNodeFactory.instance.objectNode();
        linkedHashMap.forEach(jsonLinkedHashMap::put);
        return jsonLinkedHashMap.toString();
    }
}
