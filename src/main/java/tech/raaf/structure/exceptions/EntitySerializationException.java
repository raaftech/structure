package tech.raaf.structure.exceptions;

import java.io.Serial;

public class EntitySerializationException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  public EntitySerializationException(String msg) {
    super(msg);
  }
}
