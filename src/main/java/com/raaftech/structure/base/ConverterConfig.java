package com.raaftech.structure.base;

import com.raaftech.structure.graph.converters.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.converter.Converter;

import java.util.HashSet;
import java.util.Set;

/**
 * Give a description of ConverterConfig here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-05
 */
@Configuration
public class ConverterConfig {

    @Bean
    public ConversionServiceFactoryBean conversionService() {
        Set<Converter> converters = new HashSet<>();

        converters.add(new LinkedHashMapWriter());
        converters.add(new LinkedHashMapReader());
        converters.add(new JsonSchemaBuilderWriter());
        converters.add(new JsonSchemaBuilderReader());
        converters.add(new LocalDateTimeWriter());
        converters.add(new LocalDateTimeReader());

        ConversionServiceFactoryBean bean = new ConversionServiceFactoryBean();
        bean.setConverters(converters);
        return bean;
    }

}
