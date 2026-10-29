package tech.raaf.structure.exceptions;

import java.io.Serial;

public class EntityDeserializationException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  public EntityDeserializationException(String msg) {
    super(msg);
  }
}
