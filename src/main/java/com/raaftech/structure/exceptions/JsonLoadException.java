package com.raaftech.structure.exceptions;

/**
 * This exception is thrown when we can't load JSON, possibly because of syntax errors.
 */
public class JsonLoadException extends RuntimeException {

    /**
     * The default constructor.
     * @param message The message to be shown.
     */
    public JsonLoadException(String message) {
        super(message);
    }

}
