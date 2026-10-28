package tech.raaf.structure.exceptions;

import java.io.Serial;

/**
 * A database error. Graph changes the database exceptions to this one.
 */
public class GraphException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  public GraphException(String message) {
    super(message);
  }

  public GraphException(Throwable cause) {
    super(cause);
  }
}
