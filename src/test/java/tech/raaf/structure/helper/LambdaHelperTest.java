package tech.raaf.structure.helper;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LambdaHelperTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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
    expectedException.expect(RuntimeException.class);
    LambdaHelper.wrapWithExceptionPropagation(() -> {
      throw new RuntimeException("foo");
    }).get();
  }

  @Test
  public void wrapWithExceptionPropagationException() throws Exception {
    expectedException.expect(RuntimeException.class);
    LambdaHelper.wrapWithExceptionPropagation(() -> {
      throw new Exception("foo");
    }).get();
  }
}
