package com.raaftech.structure.graph.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raaftech.structure.base.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

import java.io.IOException;

/**
 * ConstraintReader converts from (JSON) String to JsonNode
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-08
 */
public class JsonNodeReader implements Converter<String, JsonNode> {

	private Logger log = LoggerFactory.getLogger(JsonNodeReader.class);

	@Override
	public JsonNode convert(String s) {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = null;
		try {
			json = mapper.readTree(s);
		} catch (IOException e) {
			log.error(e.getMessage());

			if (Configuration.isTraceModeOn()) {
				e.printStackTrace();
			}
		}
		return json;
	}
}
