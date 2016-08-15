package com.raaftech.structure.graph.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * Give a description of LinkedHashMapReader here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-01-26
 */
public class LinkedHashMapReader implements Converter<String, LinkedHashMap<String, String>> {

	private Logger log = LoggerFactory.getLogger(LinkedHashMapReader.class);

	@Override
	public LinkedHashMap<String, String> convert(String s) {
		ObjectMapper mapper = new ObjectMapper();
		TypeReference ref = new TypeReference<LinkedHashMap<String, String>>() {};
		try {
			return mapper.readValue(s, ref);
		} catch (IOException e) {
			log.error(e.getMessage());

			if (log.isTraceEnabled()) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
