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

    /**
     * Convert this {@link FunctionalStream} of one type to another
     * by using the mapper {@link Function} provided. It will be applied
     * to every element of the current {@link FunctionalStream}.
     *
     * @param <K> the new type of the {@link FunctionalStream}
     * @param mapper the mapper {@link Function} to use
     * @return the new {@link FunctionalStream}
     */
    <K> FunctionalStream<K> map(Function<? super T, K> mapper);

    /**
     * Convert a {@link FunctionalStream} of {@link FunctionalStream}
     * to a {@link FunctionalStream} by applying every element of the
     * containing {@link FunctionalStream} to the new {@link FunctionalStream}.
     *
     * @param <K> the new type of the {@link FunctionalStream}
     * @param mapper the mapper {@link Function} to use and get the {@link FunctionalStream} to get the data from
     * @return the new {@link FunctionalStream}
     */
    <K> FunctionalStream<K> flatMap(Function<? super T, FunctionalStream<K>> mapper);

    /**
     * Retain anything in this {@link FunctionalStream} that matched
     * the given {@link Predicate}. Drops every Element not matching it.
     *
     * @param filter the {@link Predicate} to use
     * @return the new {@link FunctionalStream}
     */
    FunctionalStream<T> filter(Predicate<? super T> filter);

    /**
     * Retain every unique element of this {@link FunctionalStream}.
     * It uses a {@link HashSet} to check if the current Element at
     * hand was already seen or not. Therefore this implementation
     * needs O(n) memory, where n is the size of the {@link FunctionalStream}.
     *
     * @return the new {@link FunctionalStream}
     */
    default FunctionalStream<T> distinct() {
        Set<T> set = new HashSet<>();
        return filter(set::add);
    }

    /**
     * Unwrap a value that can be an {@link Optional} to the containing
     * value by filtering on {@link Optional#isPresent()} and than
     * calling {@link Optional#get()}. To get the {@link Optional} in
     * the first place on every Element of this {@link FunctionalStream}
     * the so called 'testFunction' {@link Function} gets applied. This
     * {@link Function} return an {@link Optional} that is then unwrapped
     * by the successive calls.
     *
     * @param <K> the new type of the {@link FunctionalStream}
     * @param testFunction the mapper {@link Function} to use
     * @return the new {@link FunctionalStream}
     */
    default <K> FunctionalStream<K> unwrap(Function<T, Optional<K>> testFunction) {
        return map(testFunction).filter(Optional::isPresent).map(Optional::get);
    }

    /**
     * Retain any Element in this {@link FunctionalStream} that is not
     * {@code null} and of type 'type'.
     *
     * @param <K> the new type of the {@link FunctionalStream}
     * @param type the {@link Class} type to retain in the {@link FunctionalStream}
     * @return the new {@link FunctionalStream}
     */
    default <K> FunctionalStream<K> ofType(Class<K> type) {
        return filter(Objects::nonNull).filter(t -> type.isAssignableFrom(t.getClass())).map(type::cast);
    }

    // TODO: JavaDoc
    default <K> FunctionalStream<K> tap(Function<FunctionalStream<T>, FunctionalStream<K>> tappingFunction) {
        return tappingFunction.apply(this);
    }

    /**
     * This can be used to debug the {@link FunctionalStream} or
     * call one specific thing on every Element of the {@link FunctionalStream}.
     *
     * @param consumer the {@link Consumer} to call
     * @return the new {@link FunctionalStream}
     */
    FunctionalStream<T> peek(Consumer<? super T> consumer);

    /**
     * This can be used to debug the {@link FunctionalStream} and get
     * every element in a {@link List}.
     *
     * @param list the {@link List} to put every Element into
     * @return the new {@link FunctionalStream}
     */
    default FunctionalStream<T> peekResult(List<T> list) {
        return peek(list::add);
    }

    /**
     * This can be used to debug the {@link FunctionalStream} and get
     * every element in a {@link Set}.
     *
     * @param set the {@link Set} to put every Element into
     * @return the new {@link FunctionalStream}
     */
    default FunctionalStream<T> peekResult(Set<T> set) {
        return peek(set::add);
    }

    /**
     * Limit this {@link FunctionalStream} to n Elements and retain
     * only those.
     *
     * @param count the retaining count
     * @return the new {@link FunctionalStream}
     */
    FunctionalStream<T> limit(long count);

    /**
     * Skip n Elements of this {@link FunctionalStream} and retain
     * any after the skipped Elements.
     *
     * @param count the skipping count
     * @return the new {@link FunctionalStream}
     */
    FunctionalStream<T> skip(long count);

    /**
     * Terminate this {@link FunctionalStream} and apply the
     * given {@link Consumer} to every Element left in the
     * {@link FunctionalStream}.
     *
     * @param consumer the {@link Consumer} to use
     */
    void forEach(Consumer<? super T> consumer);

    /**
     * Terminate this {@link FunctionalStream} and collect
     * every Element left in this {@link FunctionalStream}
     * into a {@link List}.
     *
     * @return the {@link List} of Elements
     */
    List<T> toList();

    /**
     * Terminate this {@link FunctionalStream} and collect
     * every Element left in this {@link FunctionalStream}
     * into a {@link Set}.
     *
     * @return the {@link Set} of Elements
     */
    Set<T> toSet();

    /**
     * Terminate this {@link FunctionalStream} and collect
     * every Element left in this {@link FunctionalStream}
     * into a {@link String} with a specific delimiter.
     *
     * @param delimiter the delimiter to use
     * @return the {@link String} of every Element joined by the delimiter
     */
    String joining(String delimiter);

    /**
     * Terminate this {@link FunctionalStream} without
     * any return value.
     */
    void eval();

    /**
     * Terminate this {@link FunctionalStream} and
     * check if one Element can be found that qualifies to
     * the {@link Predicate} given. The {@link Predicate}
     * does not need to be evaluated on every Element to
     * determine the output of this call.
     *
     * @param predicate the {@link Predicate} to use
     * @return if any Element matched the {@link Predicate}
     */
    boolean anyMatch(Predicate<? super T> predicate);

    /**
     * Terminate this {@link FunctionalStream} and
     * check if every Element qualifies to the
     * {@link Predicate} given. The {@link Predicate}
     * does not need to be evaluated on every Element
     * to determine the output of this call.
     *
     * @param predicate the {@link Predicate} to use
     * @return if every Element matched the {@link Predicate}
     */
    boolean allMatch(Predicate<? super T> predicate);

    /**
     * Terminate this {@link FunctionalStream} and
     * check if no Element qualified to the
     * {@link Predicate} given. The {@link Predicate}
     * does not need to be evaluated on every Element
     * to determine the output of this call.
     *
     * @param predicate the {@link Predicate} to use
     * @return if no Element matched the {@link Predicate}
     */
    boolean noneMatch(Predicate<? super T> predicate);

    /**
     * Terminate this {@link FunctionalStream} and
     * count the Elements left in this
     * {@link FunctionalStream}.
     *
     * @return the Element count of this {@link FunctionalStream}
     */
    default long count() {
        return longSum(t -> 1L);
    }

    /**
     * Terminate this {@link FunctionalStream} without
     * evaluating anything.
     */
    void close();

    /**
     * Terminate this {@link FunctionalStream} and return
     * the first Element in this {@link FunctionalStream}.
     * If this {@link FunctionalStream} is empty
     * {@link Optional#empty()} gets returned.
     *
     * @return the first Element of this {@link FunctionalStream} or none.
     */
    Optional<T> findFirst();

    /**
     * Terminate this {@link FunctionalStream} and return
     * the smallest Element determined by the {@link Comparator}
     * given. If this {@link FunctionalStream} is empty
     * {@link Optional#empty()} gets returned.
     *
     * @param comparator the {@link Comparator} to compare the Elements
     * @return the smallest Element of this {@link FunctionalStream} or none.
     */
    Optional<T> min(Comparator<T> comparator);

    /**
     * Terminate this {@link FunctionalStream} and return
     * the biggest Element determined by the {@link Comparator}
     * given. If this {@link FunctionalStream} is empty
     * {@link Optional#empty()} gets returned.
     *
     * @param comparator the {@link Comparator} to compare the Elements
     * @return the biggest Element of this {@link FunctionalStream} or none.
     */
    Optional<T> max(Comparator<T> comparator);

    // TODO: JavaDoc
    <R, A> R collect(Collector<? super T, A, R> collector);

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Float}. The {@link Function} will map every
     * Element to type {@link Float}.
     *
     * @param floatFunction the {@link Function} to produce the {@link Float}'s
     * @return the sum of every {@link Float}
     */
    default float floatSum(Function<T, Float> floatFunction) {
        return map(floatFunction).reduce(0.0F, Float::sum);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Integer}. The {@link Function} will map every
     * Element to type {@link Integer}.
     *
     * @param integerFunction the {@link Function} to produce the {@link Integer}'s
     * @return the sum of every {@link Integer}
     */
    default int integerSum(Function<T, Integer> integerFunction) {
        return map(integerFunction).reduce(0, Integer::sum);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Double}. The {@link Function} will map every
     * Element to type {@link Double}.
     *
     * @param doubleFunction the {@link Function} to produce the {@link Double}'s
     * @return the sum of every {@link Double}
     */
    default double doubleSum(Function<T, Double> doubleFunction) {
        return map(doubleFunction).reduce(0.0D, Double::sum);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Long}. The {@link Function} will map every
     * Element to type {@link Long}.
     *
     * @param longFunction the {@link Function} to produce the {@link Long}'s
     * @return the sum of every {@link Long}
     */
    default long longSum(Function<T, Long> longFunction) {
        return map(longFunction).reduce(0L, Long::sum);
    }

    /**
     * @return a {@link Stream} of this {@link FunctionalStream}.
     */
    Stream<T> toStream();

    /**
     * Terminate this {@link FunctionalStream} and return an
     * array of Elements of the {@link FunctionalStream}.
     *
     * @param intFunction the array creation function
     * @return the array with every Element
     */
    T[] toArray(IntFunction<T[]> intFunction);

    /**
     * Terminate this {@link FunctionalStream} and return
     * a single return Element determined by a given
     * 'identity' and an {@link BinaryOperator} to mutate
     * this initial 'identity' until every Element is
     * used.
     *
     * @param identity the initial value
     * @param accumulator the accumulator to mutate the value
     * @return the single return Element
     */
    T reduce(T identity, BinaryOperator<T> accumulator);

}
