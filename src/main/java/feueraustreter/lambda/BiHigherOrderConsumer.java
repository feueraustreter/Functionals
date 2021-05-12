package feueraustreter.lambda;

import java.util.function.BiFunction;
import java.util.function.Consumer;

@FunctionalInterface
public interface BiHigherOrderConsumer<T> extends BiFunction<T, T, Consumer<T>> {
}
