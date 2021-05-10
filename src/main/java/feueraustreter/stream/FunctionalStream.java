package feueraustreter.stream;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface FunctionalStream<T> {

    static <K> FunctionalStream<K> of(Iterable<K> iterable) {
        return new FunctionalStreamImpl<>(iterable.iterator());
    }

    static <K> FunctionalStream<K> of(Iterator<K> iterator) {
        return new FunctionalStreamImpl<>(iterator);
    }

    static <K> FunctionalStream<K> of(Stream<K> stream) {
        return new FunctionalStreamImpl<>(stream.iterator());
    }

    <K> FunctionalStream<K> map(Function<T, K> mapper);

    <K> FunctionalStream<K> flatMap(Function<T, FunctionalStream<K>> mapper);

    FunctionalStream<T> filter(Predicate<T> filter);

    <K> FunctionalStream<K> tap(Function<FunctionalStream<T>, FunctionalStream<K>> tappingFunction);

    FunctionalStream<T> peek(Consumer<T> consumer);

    FunctionalStream<T> limit(long count);

    FunctionalStream<T> skip(long count);

    void forEach(Consumer<T> consumer);

    List<T> toList();

    Set<T> toSet();

    String joining(String delimiter);

    void eval();

}
