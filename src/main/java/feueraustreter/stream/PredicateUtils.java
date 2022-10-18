package feueraustreter.stream;

import lombok.experimental.UtilityClass;

import java.util.function.Predicate;

@UtilityClass
public class PredicateUtils {

    public static <T> Predicate<T> equal(T l) {
        return Predicate.isEqual(l);
    }

    public static Predicate<Long> greater(Long number) {
        return current -> current > number;
    }

    public static Predicate<Long> less(Long number) {
        return current -> current < number;
    }

    public static Predicate<Integer> greater(Integer number) {
        return current -> current > number;
    }

    public static Predicate<Integer> less(Integer number) {
        return current -> current < number;
    }

    public static Predicate<Float> greater(Float number) {
        return current -> current > number;
    }

    public static Predicate<Float> less(Float number) {
        return current -> current < number;
    }

    public static Predicate<Double> greater(Double number) {
        return current -> current > number;
    }

    public static Predicate<Double> less(Double number) {
        return current -> current < number;
    }
}
