package tech.raaf.structure.exceptions;

import java.io.Serial;

public class ResultBuilderException extends IllegalStateException {

  @Serial
  private static final long serialVersionUID = 1L;

  public ResultBuilderException(String message) {
    super(message);
  }
}
