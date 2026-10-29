package tech.raaf.structure.exceptions;

import java.io.Serial;

public class InstrumentExecutionException extends Exception {

  @Serial
  private static final long serialVersionUID = 1L;

  public InstrumentExecutionException(String message) {
    super(message);
  }
}
