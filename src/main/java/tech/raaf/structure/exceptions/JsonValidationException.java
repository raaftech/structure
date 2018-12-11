package tech.raaf.structure.exceptions;

/**
 * This exception is thrown when we unsuccesfully try to validate JSON against a JSON schema.
 */
public class JsonValidationException extends RuntimeException {

    /**
     * The constructor.
     * @param message The message to be given along with the exception.
     */
    public JsonValidationException(String message) {
        super(message);
    }

}
