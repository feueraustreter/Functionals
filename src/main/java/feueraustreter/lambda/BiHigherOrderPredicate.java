package feueraustreter.lambda;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public interface BiHigherOrderPredicate<T> extends BiFunction<T, T, Predicate<T>> {
}
