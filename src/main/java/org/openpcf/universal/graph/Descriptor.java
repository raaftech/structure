package org.openpcf.universal.graph;

import java.security.InvalidParameterException;

/**
 * Give a description of Constraint here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-05
 */
public class Descriptor {

    private String kvType;
    private Boolean required;
    private String description;
    private String defaultValue;
    private String exampleValue;

    public Descriptor(String kvType, Boolean required, String description, String defaultValue, String exampleValue) {
        this.required = required;
        this.description = description;
        this.defaultValue = defaultValue;
        this.exampleValue = exampleValue;

        switch (kvType) {
            case "string":
            case "integer":
            case "double":
            case "datetime":
            case "boolean":
                this.kvType = kvType;
                break;
            default:
                throw new InvalidParameterException("kvType must be of value string, integer, datetime or boolean.");
        }
    }

    public String getKvType() {
        return kvType;
    }

    public Boolean getRequired() {
        return required;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getExampleValue() {
        return exampleValue;
    }
}
