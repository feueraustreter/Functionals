package feueraustreter.lambda;

import java.util.function.Consumer;
import java.util.function.Function;

@FunctionalInterface
public interface HigherOrderConsumer<T> extends Function<T, Consumer<T>> {
}
