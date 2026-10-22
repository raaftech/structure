package tech.raaf.structure.exceptions;

/**
 * A database error. Graph changes the database exceptions to this one.
 */
public class GraphException extends RuntimeException {

  public GraphException(String message) {
    super(message);
  }

  public GraphException(Throwable cause) {
    super(cause);
  }
}
