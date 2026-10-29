package tech.raaf.structure.exceptions;

import java.io.Serial;

public class ComponentBuilderException extends IllegalStateException {

  @Serial
  private static final long serialVersionUID = 1L;

  public ComponentBuilderException(String message) {
    super(message);
  }
}
