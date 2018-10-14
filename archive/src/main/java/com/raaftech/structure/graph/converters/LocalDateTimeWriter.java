package com.raaftech.structure.graph.converters;

import org.springframework.core.convert.converter.Converter;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class LocalDateTimeWriter implements Converter<LocalDateTime, Long> {

    @Override
    public Long convert(LocalDateTime localDateTime) {
        ZoneId zoneId = ZoneId.systemDefault();
        return localDateTime.toInstant(zoneId.getRules().getOffset(localDateTime)).toEpochMilli();
    }

}
