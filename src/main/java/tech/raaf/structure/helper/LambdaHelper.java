package tech.raaf.structure.helper;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LambdaHelper {

    private LambdaHelper() {
    }

    /**
     * Call a method that might throw an exception, handling the exception if it does.
     * If no exception was thrown we return the return value
     * wrapped in an {@link java.util.Optional Optional}. If the exception was thrown, run {@code error} and
     * return an empty optional.
     * This method is mainly used to avoid having to use {@code try} blocks inside lambda's.
     *
     * @param callable The wrapped method.
     * @param error    The error handler.
     * @param <T>      The return type.
     * @return {@link java.util.Optional Optionally} the return value of {@code callable}.
     */
    public static <T> Optional<T> wrapWithExceptionHandler(Callable<T> callable, Consumer<Throwable> error) {
        Optional<T> ret;
        try {
            ret = Optional.of(callable.call());
        } catch (Exception e) {
            ret = Optional.empty();
            if (error != null) {
                error.accept(e);
            }
        }
        return ret;
    }

    /**
     * Call a method that might throw an exception, rethrowing the exception as a {@link java.lang.RuntimeException}
     * if it does. If no exception was thrown,
     * we return the return value wrapped by a {@link java.util.function.Supplier Supplier}. Otherwise, we rethrow
     * the exception as a {@link java.lang.RuntimeException}.
     * Like {@link #wrapWithExceptionHandler(Callable, Consumer)}, this method is mainly used to avoid having to use
     * {@code try} blocks inside lambda's.
     *
     * @param callable The wrapped method.
     * @param <T>      The return type.
     * @return A {@link java.util.function.Supplier Supplier} holding the return value of {@code callable}.
     */
    public static <T> Supplier<T> wrapWithExceptionPropagation(Callable<T> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
