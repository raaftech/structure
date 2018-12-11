package tech.raaf.structure.exceptions;

/**
 * InstrumentNotFoundException is thrown when a package refers to an instrument that doesn't exist.
 */
public class InstrumentNotFoundException extends RuntimeException {

    /**
     * The constructor.
     * @param message What message to show.
     */
    public InstrumentNotFoundException(String message) {
        super(message);
    }

}
