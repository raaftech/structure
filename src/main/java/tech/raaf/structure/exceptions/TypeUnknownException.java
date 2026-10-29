package tech.raaf.structure.exceptions;

import java.io.Serial;

/**
 * This exception is thrown when referencing an unknown Type.
 */
public class TypeUnknownException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * The default constructor.
   * @param message The message to show.
   */
  public TypeUnknownException(String message) {
    super(message);
  }
}
