package feueraustreter.lambda;

import java.util.function.Function;

@FunctionalInterface
public interface HigherOrderFunction<T, K> extends Function<T, Function<T, K>> {
}
