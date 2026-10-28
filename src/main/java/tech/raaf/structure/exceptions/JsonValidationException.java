package tech.raaf.structure.exceptions;

import java.io.Serial;

/**
 * This exception is thrown when we unsuccesfully try to validate JSON against a JSON schema.
 */
public class JsonValidationException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * The constructor.
   * @param message The message to be given along with the exception.
   */
  public JsonValidationException(String message) {
    super(message);
  }
}
