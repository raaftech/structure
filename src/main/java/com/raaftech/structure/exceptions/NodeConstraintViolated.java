package com.raaftech.structure.exceptions;


/**
 * An exception that is thrown when we violate a database constraint.
 */
public class NodeConstraintViolated extends RuntimeException {

    /**
     * The default constructor.
     * @param message The message to be shown.
     */
    public NodeConstraintViolated(String message) {
        super(message);
    }

}
