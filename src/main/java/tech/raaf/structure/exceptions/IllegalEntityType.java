package tech.raaf.structure.exceptions;

import java.io.Serial;

public class IllegalEntityType extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  public IllegalEntityType(String message) {
    super(message);
  }
}
