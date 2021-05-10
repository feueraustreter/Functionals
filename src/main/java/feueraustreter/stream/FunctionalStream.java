package feueraustreter.stream;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface FunctionalStream<T> {

    static <K> FunctionalStream<K> of(Iterable<K> iterable) {
        return new FunctionalStreamImpl<K>(iterable);
    }

    <K> FunctionalStream<K> map(Function<T, K> mapper);

    FunctionalStream<T> filter(Predicate<T> filter);

    <K> FunctionalStream<K> tap(Function<FunctionalStream<T>, FunctionalStream<K>> tappingFunction);

    FunctionalStream<T> peek(Consumer<T> consumer);

    void forEach(Consumer<T> consumer);

    List<T> toList();

    Set<T> toSet();

    void eval();

}
