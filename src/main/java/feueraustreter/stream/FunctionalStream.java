package feueraustreter.stream;

import feueraustreter.lambda.BiHigherOrderFunction;
import feueraustreter.lambda.HigherOrderFunction;
import feueraustreter.lambda.ThrowableFunction;
import feueraustreter.tryfunction.Try;
import lombok.NonNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface FunctionalStream<T> extends Iterable<T> {

    // Creation methods

    /**
     * Create a {@link FunctionalStream} of an existing {@link Iterable}.
     *
     * @param <K> the {@link FunctionalStream} type to use
     * @param iterable the {@link Iterable} ElementSource
     * @return the new {@link FunctionalStream}
     */
    static <K> FunctionalStream<K> of(Iterable<K> iterable) {
        return new FunctionalStreamImpl<>(iterable.iterator());
    }

    /**
     * Create a {@link FunctionalStream} of an existing {@link Iterator}.
     *
     * @param <K> the {@link FunctionalStream} type to use
     * @param iterator the {@link Iterator} ElementSource
     * @return the new {@link FunctionalStream}
     */
    static <K> FunctionalStream<K> of(Iterator<K> iterator) {
        return new FunctionalStreamImpl<>(iterator);
    }

    /**
     * Create a {@link FunctionalStream} of an existing {@link Stream}.
     *
     * @param <K> the {@link FunctionalStream} type to use
     * @param stream the {@link Stream} ElementSource
     * @return the new {@link FunctionalStream}
     */
    static <K> FunctionalStream<K> of(Stream<K> stream) {
        return new FunctionalStreamImpl<>(stream.iterator());
    }

    // Conversion methods

    /**
     * Convert this {@link FunctionalStream} of one type to another
     * by using the mapper {@link Function} provided. It will be applied
     * to every element of the current {@link FunctionalStream}.
     *
     * @param <K> the new type of the {@link FunctionalStream}
     * @param mapper the mapper {@link Function} to use
     * @return the new {@link FunctionalStream}
     * @see Stream#map(Function) for more information regarding this method
     */
    <K> FunctionalStream<K> map(Function<? super T, K> mapper);

    // TODO: JavaDoc
    default <K> FunctionalStream<K> higherOrderMap(HigherOrderFunction<T, K> higherOrderMapper) {
        return map(t -> higherOrderMapper.apply(t).apply(t));
    }

    // TODO: JavaDoc
    default <K> FunctionalStream<K> higherOrderMapWithPrevious(T identity, HigherOrderFunction<T, K> higherOrderMapper) {
        AtomicReference<T> current = new AtomicReference<>(identity);
        return map(t -> {
            T toUse = current.getAndSet(t);
            return higherOrderMapper.apply(toUse).apply(t);
        });
    }

    // TODO: JavaDoc
    default <K> FunctionalStream<K> higherOrderMapAndPrevious(T identity, BiHigherOrderFunction<T, K> higherOrderMapper) {
        AtomicReference<T> current = new AtomicReference<>(identity);
        return map(t -> {
            T toUse = current.getAndSet(t);
            return higherOrderMapper.apply(toUse, t).apply(t);
        });
    }

    /**
     * Convert a {@link FunctionalStream} of {@link FunctionalStream}
     * to a {@link FunctionalStream} by applying every element of the
     * containing {@link FunctionalStream} to the new {@link FunctionalStream}.
     *
     * @param <K> the new type of the {@link FunctionalStream}
     * @param mapper the mapper {@link Function} to use and get the {@link FunctionalStream} to get the data from
     * @return the new {@link FunctionalStream}
     * @see Stream#flatMap(Function) for more information regarding this method
     */
    <K> FunctionalStream<K> flatMap(Function<? super T, FunctionalStream<K>> mapper);

    /**
     * Convert some Elements of this {@link FunctionalStream} to new
     * Elements of the same type. Retains every element not matching
     * the 'filter' and applies something to those that match.
     *
     * @param filter the {@link Predicate} to use
     * @param mapper thw mapper {@link Function} to use
     * @return the new {@link FunctionalStream}
     */
    FunctionalStream<T> partialMap(Predicate<? super T> filter, UnaryOperator<T> mapper);

    /**
     * Retain anything in this {@link FunctionalStream} that matched
     * the given {@link Predicate}. Drops every Element not matching it.
     *
     * @param filter the {@link Predicate} to use
     * @return the new {@link FunctionalStream}
     * @see Stream#filter(Predicate) for more information regarding this method
     */
    FunctionalStream<T> filter(Predicate<? super T> filter);

    /**
     * Retain every unique element of this {@link FunctionalStream}.
     * It uses a {@link HashSet} to check if the current Element at
     * hand was already seen or not. Therefore this implementation
     * needs O(n) memory, where n is the size of the {@link FunctionalStream}.
     *
     * @return the new {@link FunctionalStream}
     * @see Stream#distinct() for more information regarding this method
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
    default <K> FunctionalStream<K> ofType(@NonNull Class<K> type) {
        return filter(Objects::nonNull).filter(t -> type.isAssignableFrom(t.getClass())).map(type::cast);
    }

    /**
     * Apply some kind of mapping to this {@link FunctionalStream} outside
     * this caller. It can be used by an API to template a specific mapping
     * and provide it as an method to use by the user.
     *
     * @param <K> the new type of the {@link FunctionalStream}
     * @param tappingFunction the {@link Function} to convert from old to new {@link FunctionalStream}
     * @return the new {@link FunctionalStream}
     */
    default <K> FunctionalStream<K> tap(Function<FunctionalStream<T>, FunctionalStream<K>> tappingFunction) {
        return tappingFunction.apply(this);
    }

    /**
     * This can be used to debug the {@link FunctionalStream} or
     * call one specific thing on every Element of the {@link FunctionalStream}.
     *
     * @param consumer the {@link Consumer} to call
     * @return the new {@link FunctionalStream}
     * @see Stream#peek(Consumer) for more information regarding this method
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
     * @see Stream#limit(long) for more information regarding this method
     */
    FunctionalStream<T> limit(long count);

    /**
     * Skip n Elements of this {@link FunctionalStream} and retain
     * any after the skipped Elements.
     *
     * @param count the skipping count
     * @return the new {@link FunctionalStream}
     * @see Stream#skip(long) for more information regarding this method
     */
    FunctionalStream<T> skip(long count);

    // TODO: JavaDoc
    default FunctionalStream<T> keep(long from, long to) {
        if (to < from) {
            throw new IllegalArgumentException();
        }
        return skip(from).limit(to - from);
    }

    /**
     * @return a {@link Stream} of this {@link FunctionalStream}.
     */
    Stream<T> toStream();

    /**
     * Map any Element of this {@link FunctionalStream} by some {@link Function}
     * and ignore if any {@link Throwable} gets thrown. It will retain
     * every Element, that mapped without an {@link Exception}.
     *
     * @param <K> the new type of the {@link FunctionalStream}
     * @param <E> the {@link Throwable} type of the applied {@link Function}
     * @param tryFunction the Function to apply.
     * @return the new {@link FunctionalStream}
     */
    default <K, E extends Throwable> FunctionalStream<K> tryIt(Function<T, Try<K, E>> tryFunction) {
        return map(tryFunction).filter(Try::successful).map(Try::getSuccess);
    }

    /**
     * Map any Element of this {@link FunctionalStream} by some {@link Function}
     * and ignore if any {@link Throwable} gets thrown. It will retain
     * every Element, that mapped without an {@link Exception}.
     *
     * @param <K> the new type of the {@link FunctionalStream}
     * @param <E> the {@link Throwable} type of the applied {@link Function}
     * @param tryFunction the Function to apply.
     * @return the new {@link FunctionalStream}
     */
    default <K, E extends Throwable> FunctionalStream<K> tryIt(ThrowableFunction<T, E, K> tryFunction) {
        return map(t -> Try.tryIt(() -> tryFunction.apply(t))).filter(Try::successful).map(Try::getSuccess);
    }

    /**
     * Combine this {@link FunctionalStream} with another {@link FunctionalStream}
     * and return one combined {@link FunctionalStream} containing every Element
     * of both {@link FunctionalStream}. This operation is optional and can combine
     * both {@link FunctionalStream} in any way, preferably this {@link FunctionalStream}
     * before the other. If this {@link FunctionalStream} is empty the zip method
     * must be evaluated and produce a {@link FunctionalStream} of the Elements of
     * the inputted {@link FunctionalStream}.
     *
     * @param other the {@link FunctionalStream} to zip with
     * @return the new {@link FunctionalStream}
     */
    default FunctionalStream<T> zip(FunctionalStream<T> other) {
        throw new UnsupportedOperationException();
    }

    // Terminating methods

    /**
     * Terminate this {@link FunctionalStream} and apply the
     * given {@link Consumer} to every Element left in the
     * {@link FunctionalStream}.
     *
     * @param consumer the {@link Consumer} to use
     * @see Stream#forEach(Consumer) for more information regarding this method
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
     * Terminate and evaluate every statement of this {@link FunctionalStream}
     * without any return value.
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
     * @see Stream#anyMatch(Predicate) for more information regarding this method
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
     * @see Stream#allMatch(Predicate) for more information regarding this method
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
     * @see Stream#noneMatch(Predicate) for more information regarding this method
     */
    boolean noneMatch(Predicate<? super T> predicate);

    /**
     * Terminate this {@link FunctionalStream} and
     * count the Elements left in this
     * {@link FunctionalStream}.
     *
     * @return the Element count of this {@link FunctionalStream}
     * @see Stream#count() for more information regarding this method
     */
    default long count() {
        return longSum(t -> 1L);
    }

    /**
     * Terminate this {@link FunctionalStream} without
     * evaluating anything.
     *
     * @see Stream#close() for more information regarding this method
     */
    void close();

    /**
     * Terminate this {@link FunctionalStream} and return
     * the first Element in this {@link FunctionalStream}.
     * If this {@link FunctionalStream} is empty
     * {@link Optional#empty()} gets returned.
     *
     * @return the first Element of this {@link FunctionalStream} or none.
     * @see Stream#findFirst() for more information regarding this method
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
     * @see Stream#min(Comparator) for more information regarding this method
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
     * @see Stream#max(Comparator) for more information regarding this method
     */
    Optional<T> max(Comparator<T> comparator);

    /**
     * Terminate this {@link FunctionalStream} and collect it
     * to a specific type by using a {@link Collector}. Some
     * common {@link Collector}'s can be found in the
     * {@link Collectors} class.
     *
     * @param <R> the type of the result
     * @param <A> the intermediate accumulation type of the {@link Collector}
     * @param collector the {@link Collector} describing the reduction
     * @return the result of the reduction
     * @see Collectors
     * @see Stream#collect(Collector) for more information regarding this method
     */
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
     * Terminate this {@link FunctionalStream} and return an
     * array of Elements of the {@link FunctionalStream}.
     *
     * @param intFunction the array creation function
     * @return the array with every Element
     * @see Stream#toArray(IntFunction) for more information regarding this method
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
     * @see Stream#reduce(Object, BinaryOperator) for more information regarding this method
     */
    T reduce(T identity, BinaryOperator<T> accumulator);

    // API for common use cases, like zip()

    /**
     * This is used as a common API for the implementation. Calling from outside should
     * not be done.
     *
     * @return if this stream has at least one Element left
     */
    default boolean hasNext() {
        throw new UnsupportedOperationException();
    }

    /**
     * This is used as a common API for the implementation. Calling from outside should
     * not be done.
     */
    default void evalNext() {
        throw new UnsupportedOperationException();
    }

}
