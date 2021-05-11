package feueraustreter.stream;

@FunctionalInterface
public interface ThrowableFunction<T, E extends Throwable, R> {
    R apply(T t) throws E;
}
