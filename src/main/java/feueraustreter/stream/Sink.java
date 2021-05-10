package feueraustreter.stream;

import java.util.function.Consumer;

@FunctionalInterface
public interface Sink<T> extends Consumer<T> {
}
