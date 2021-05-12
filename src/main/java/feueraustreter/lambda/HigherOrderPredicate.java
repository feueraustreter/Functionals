package feueraustreter.lambda;

import java.util.function.Function;
import java.util.function.Predicate;

@FunctionalInterface
public interface HigherOrderPredicate<T> extends Function<T, Predicate<T>> {
}
