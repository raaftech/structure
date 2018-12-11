package tech.raaf.structure.exceptions;

/**
 * This exception is thrown when referencing an unknown Type.
 */
public class TypeUnknownException extends RuntimeException {

    /**
     * The default constructor.
     * @param message The message to show.
     */
    public TypeUnknownException(String message) {
        super(message);
    }

}
