package com.raaftech.structure.exceptions;

/**
 * Thrown when running into IO errors while loading a package.
 */
public class PackageIOException extends RuntimeException {

    /**
     * The default constructor.
     * @param message The message to be shown.
     */
    public PackageIOException(String message) {
        super(message);
    }

}
