package tech.raaf.structure.exceptions;

import java.io.Serial;

public class FileInjectionException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  public FileInjectionException(String message) {
    super(message);
  }
}
