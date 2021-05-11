package feueraustreter.stream;

import java.util.function.Consumer;

/**
 * This class is used internally and could be represented by a {@link Consumer}
 * but was not as this more clearly depicts what this class should do. It accepts
 * any element of the previous operation and can do something to it if necessary.
 *
 * @param <T> the type it accepts.
 */
@FunctionalInterface
public interface Sink<T> {
    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    void accept(T t);
}
