package org.openpcf.universal.base;

import java.util.HashSet;
import java.util.Set;

import org.openpcf.universal.graph.converters.DescriptorReader;
import org.openpcf.universal.graph.converters.DescriptorWriter;
import org.openpcf.universal.graph.converters.JsonSchemaBuilderReader;
import org.openpcf.universal.graph.converters.JsonSchemaBuilderWriter;
import org.openpcf.universal.graph.converters.LinkedHashMapReader;
import org.openpcf.universal.graph.converters.LinkedHashMapWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.converter.Converter;

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

        converters.add(new DescriptorReader());
        converters.add(new DescriptorWriter());
        converters.add(new LinkedHashMapWriter());
        converters.add(new LinkedHashMapReader());
        converters.add(new JsonSchemaBuilderWriter());
        converters.add(new JsonSchemaBuilderReader());

        ConversionServiceFactoryBean bean = new ConversionServiceFactoryBean();
        bean.setConverters(converters);
        return bean;
    }

}
