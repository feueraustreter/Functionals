package feueraustreter.lambda;

import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface BiHigherOrderFunction<T, K> extends BiFunction<T, T, Function<T, K>> {
}
