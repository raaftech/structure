package com.raaftech.structure.exceptions;

/**
 * PackageNotFoundException is thrown when a bundle refers to a package that doesn't exist.
 */
public class PackageNotFoundException extends RuntimeException {

    /**
     * The constructor.
     * @param message What message to show.
     */
    public PackageNotFoundException(String message) {
        super(message);
    }

}
