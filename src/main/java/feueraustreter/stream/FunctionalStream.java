package feueraustreter.stream;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;

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

    <K> FunctionalStream<K> map(Function<? super T, K> mapper);

    <K> FunctionalStream<K> flatMap(Function<? super T, FunctionalStream<K>> mapper);

    FunctionalStream<T> filter(Predicate<? super T> filter);

    default FunctionalStream<T> distinct() {
        Set<T> set = new HashSet<>();
        return filter(set::add);
    }

    default <K> FunctionalStream<K> unwrap(Function<T, Optional<K>> testFunction) {
        return map(testFunction).filter(Optional::isPresent).map(Optional::get);
    }

    default <K> FunctionalStream<K> ofType(Class<K> type) {
        return filter(t -> type.isAssignableFrom(t == null ? null : t.getClass())).map(type::cast);
    }

    default <K> FunctionalStream<K> tap(Function<FunctionalStream<T>, FunctionalStream<K>> tappingFunction) {
        return tappingFunction.apply(this);
    }

    FunctionalStream<T> peek(Consumer<? super T> consumer);

    default FunctionalStream<T> peekResult(List<T> list) {
        return peek(list::add);
    }

    default FunctionalStream<T> peekResult(Set<T> set) {
        return peek(set::add);
    }

    FunctionalStream<T> limit(long count);

    FunctionalStream<T> skip(long count);

    void forEach(Consumer<? super T> consumer);

    List<T> toList();

    Set<T> toSet();

    String joining(String delimiter);

    void eval();

    boolean anyMatch(Predicate<? super T> predicate);

    boolean allMatch(Predicate<? super T> predicate);

    boolean noneMatch(Predicate<? super T> predicate);

    default long count() {
        return longSum(t -> 1L);
    }

    void close();

    Optional<T> findFirst();

    Optional<T> min(Comparator<T> comparator);

    Optional<T> max(Comparator<T> comparator);

    <R, A> R collect(Collector<? super T, A, R> collector);

    default float floatSum(Function<T, Float> floatFunction) {
        return map(floatFunction).reduce(0.0F, Float::sum);
    }

    default int integerSum(Function<T, Integer> integerFunction) {
        return map(integerFunction).reduce(0, Integer::sum);
    }

    default double doubleSum(Function<T, Double> doubleFunction) {
        return map(doubleFunction).reduce(0.0D, Double::sum);
    }

    default long longSum(Function<T, Long> longFunction) {
        return map(longFunction).reduce(0L, Long::sum);
    }

    Stream<T> toStream();

    T[] toArray(IntFunction<T[]> intFunction);

    T reduce(T identity, BinaryOperator<T> accumulator);

}
