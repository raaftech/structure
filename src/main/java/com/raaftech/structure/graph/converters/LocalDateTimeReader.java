package com.raaftech.structure.graph.converters;

import org.springframework.core.convert.converter.Converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class LocalDateTimeReader implements Converter<Long, LocalDateTime> {

    @Override
    public LocalDateTime convert(Long aLong) {
        return Instant.ofEpochMilli(aLong).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

}
