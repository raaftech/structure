package tech.raaf.structure.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class LambdaHelperTest {

  @Test
  public void wrapWithExceptionHandler() throws Exception {
    final AtomicBoolean handlerCalled = new AtomicBoolean(false);
    Optional<Object> optional;

    optional = LambdaHelper.wrapWithExceptionHandler(
      () -> {
        throw new Exception("foo");
      },
      e -> handlerCalled.set(true)
    );
    assertTrue(handlerCalled.get());
    assertFalse(optional.isPresent());

    optional = LambdaHelper.wrapWithExceptionHandler(() -> {
      throw new Exception("foo");
    }, null);
    assertFalse(optional.isPresent());

    optional = LambdaHelper.wrapWithExceptionHandler(null, null);
    assertFalse(optional.isPresent());

    optional = LambdaHelper.wrapWithExceptionHandler(() -> "foo", null);
    assertTrue(optional.isPresent());
    assertTrue(Objects.equals("foo", optional.get()));
  }

  @Test
  public void wrapWithExceptionHandlerCollectList() throws Exception {
    List<String> source = Arrays.asList("foo", "bar", null, "baz", null, "deadbeef");
    AtomicInteger errors = new AtomicInteger(0);
    List<String> transformed = source
      .stream()
      .map(s -> LambdaHelper.wrapWithExceptionHandler(() -> s.toUpperCase(), e -> errors.getAndIncrement()))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toList());

    assertEquals(4, transformed.size());
    assertEquals(2, errors.get());
  }

  @Test
  public void wrapWithExceptionPropagation() throws Exception {
    Supplier<Object> supplier;

    supplier = LambdaHelper.wrapWithExceptionPropagation(() -> "foo");
    assertTrue(Objects.equals("foo", supplier.get()));
  }

  @Test
  public void wrapWithExceptionPropagationRuntimeException() throws Exception {
    Supplier<Object> supplier = LambdaHelper.wrapWithExceptionPropagation(() -> {
      throw new RuntimeException("foo");
    });
    assertThrows(RuntimeException.class, supplier::get);
  }

  @Test
  public void wrapWithExceptionPropagationException() throws Exception {
    Supplier<Object> supplier = LambdaHelper.wrapWithExceptionPropagation(() -> {
      throw new Exception("foo");
    });
    assertThrows(RuntimeException.class, supplier::get);
  }
}
