/*
 * Copyright 2021 feueraustreter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package feueraustreter.stream;

import feueraustreter.lambda.*;
import feueraustreter.tryfunction.Try;
import lombok.NonNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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

    /**
     * Create a {@link FunctionalStream} of an existing {@link Map#entrySet()}.
     *
     * @param <K> the {@link FunctionalStream} type to use
     * @param map the {@link Map#entrySet()} ElementSource
     * @return the new {@link FunctionalStream}
     */
    static <K, V> FunctionalStream<Map.Entry<K, V>> of(Map<K, V> map) {
        return new FunctionalStreamImpl<>(map.entrySet().iterator());
    }

    /**
     * Returns an empty sequential {@link FunctionalStream}.
     *
     * @param <K> the type of stream elements
     * @return an empty sequential {@link FunctionalStream}
     */
    static <K> FunctionalStream<K> empty() {
        return new FunctionalStreamImpl<>(new Iterator<K>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public K next() {
                return null;
            }
        });
    }

    /**
     * Returns a sequential {@link FunctionalStream} containing a single element.
     *
     * @param element the single element
     * @param <K> the type of stream elements
     * @return a singleton sequential {@link FunctionalStream}
     */
    static <K> FunctionalStream<K> of(K element) {
        AtomicBoolean atomicBoolean = new AtomicBoolean(true);
        return new FunctionalStreamImpl<>(new Iterator<K>() {
            @Override
            public boolean hasNext() {
                return atomicBoolean.get();
            }

            @Override
            public K next() {
                atomicBoolean.set(false);
                return element;
            }
        });
    }

    /**
     * Returns a sequential ordered {@link FunctionalStream} whose elements are the specified values.
     *
     * @param <K> the type of stream elements
     * @param elements the elements of the new stream
     * @return the new {@link FunctionalStream}
     */
    @SafeVarargs
    @SuppressWarnings("varargs") // Creating a stream from an array is safe
    static <K> FunctionalStream<K> of(K... elements) {
        return new FunctionalStreamImpl<>(Arrays.stream(elements).iterator());
    }

    /**
     * Returns an infinite sequential ordered {@link FunctionalStream} produced by iterative
     * application of a function {@code f} to an initial element {@code seed},
     * producing a {@link FunctionalStream} consisting of {@code seed}, {@code f(seed)},
     * {@code f(f(seed))}, etc.
     *
     * <p>The first element (position {@code 0}) in the {@link FunctionalStream} will be
     * the provided {@code seed}.  For {@code n > 0}, the element at position
     * {@code n}, will be the result of applying the function {@code f} to the
     * element at position {@code n - 1}.
     *
     * @param <K> the type of stream elements
     * @param seed the initial element
     * @param f a function to be applied to the previous element to produce a new element
     * @return a new sequential {@link FunctionalStream}
     */
    static <K> FunctionalStream<K> iterate(K seed, UnaryOperator<K> f) {
        AtomicReference<K> current = new AtomicReference<>(seed);
        return new FunctionalStreamImpl<>(new Iterator<K>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public K next() {
                K now = current.get();
                current.set(f.apply(now));
                return now;
            }
        });
    }

    /**
     * Returns a sequential ordered {@link FunctionalStream} produced by iterative
     * application of the given {@code next} function to an initial element,
     * conditioned on satisfying the given {@code hasNext} predicate.  The
     * stream terminates as soon as the {@code hasNext} predicate returns false.
     *
     * <p>{@code iterate(Object, Predicate, UnaryOperator)}
     * should produce the same sequence of elements as produced by the corresponding for-loop:
     * <pre>{@code
     *     for (T index=seed; hasNext.test(index); index = next.apply(index)) {
     *         ...
     *     }
     * }</pre>
     *
     * <p>The resulting sequence may be empty if the {@code hasNext} predicate
     * does not hold on the seed value.  Otherwise the first element will be the
     * supplied {@code seed} value, the next element (if present) will be the
     * result of applying the {@code next} function to the {@code seed} value,
     * and so on iteratively until the {@code hasNext} predicate indicates that
     * the stream should terminate.
     *
     * @param <K> the type of stream elements
     * @param seed the initial element
     * @param hasNext a predicate to apply to elements to determine when the stream must terminate.
     * @param next a function to be applied to the previous element to produce a new element
     * @return a new sequential {@link FunctionalStream}
     */
    static <K> FunctionalStream<K> iterate(K seed, Predicate<? super K> hasNext, UnaryOperator<K> next) {
        AtomicReference<K> current = new AtomicReference<>(seed);
        return new FunctionalStreamImpl<>(new Iterator<K>() {
            @Override
            public boolean hasNext() {
                return hasNext.test(current.get());
            }

            @Override
            public K next() {
                K now = current.get();
                current.set(next.apply(now));
                return now;
            }
        });
    }

    /**
     * Returns an infinite sequential unordered stream where each element is
     * generated by the provided {@code Supplier}.  This is suitable for
     * generating constant streams, streams of random elements, etc.
     *
     * @param <K> the type of stream elements
     * @param s the {@code Supplier} of generated elements
     * @return a new infinite sequential unordered {@link FunctionalStream}
     */
    static <K> FunctionalStream<K> generate(Supplier<? extends K> s) {
        return new FunctionalStreamImpl<>(new Iterator<K>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public K next() {
                return s.get();
            }
        });
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
        return map(t -> higherOrderMapper.apply(current.getAndSet(t)).apply(t));
    }

    // TODO: JavaDoc
    default <K> FunctionalStream<K> higherOrderMapAndPrevious(T identity, BiHigherOrderFunction<T, K> higherOrderMapper) {
        AtomicReference<T> current = new AtomicReference<>(identity);
        return map(t -> higherOrderMapper.apply(current.getAndSet(t), t).apply(t));
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
     * Convert some elements of this {@link FunctionalStream} to new
     * elements of the same type. Retains every element not matching
     * the 'filter' and applies something to those that match.
     *
     * @param filter the {@link Predicate} to use
     * @param mapper thw mapper {@link Function} to use
     * @return the new {@link FunctionalStream}
     */
    default FunctionalStream<T> partialMap(Predicate<? super T> filter, UnaryOperator<T> mapper) {
        return map(t -> {
            if (filter.test(t)) {
                return mapper.apply(t);
            } else {
                return t;
            }
        });
    }

    /**
     * Retain anything in this {@link FunctionalStream} that matched
     * the given {@link Predicate}. Drops every element not matching it.
     *
     * @param filter the {@link Predicate} to use
     * @return the new {@link FunctionalStream}
     * @see Stream#filter(Predicate) for more information regarding this method
     */
    FunctionalStream<T> filter(Predicate<? super T> filter);

    // TODO: JavaDoc
    default FunctionalStream<T> higherOrderFilter(HigherOrderPredicate<T> higherOrderFilter) {
        return filter(t -> higherOrderFilter.apply(t).test(t));
    }

    // TODO: JavaDoc
    default FunctionalStream<T> higherOrderFilterWithPrevious(T identity, HigherOrderPredicate<T> higherOrderFilter) {
        AtomicReference<T> current = new AtomicReference<>(identity);
        return filter(t -> higherOrderFilter.apply(current.getAndSet(t)).test(t));
    }

    // TODO: JavaDoc
    default FunctionalStream<T> higherOrderFilterAndPrevious(T identity, BiHigherOrderPredicate<T> higherOrderFilter) {
        AtomicReference<T> current = new AtomicReference<>(identity);
        return filter(t -> higherOrderFilter.apply(current.getAndSet(t), t).test(t));
    }

    /**
     * Remove anything in this {@link FunctionalStream} that matched
     * the given {@link Predicate}. Drops every element matching it.
     *
     * @param filter the {@link Predicate} to use
     * @return the new {@link FunctionalStream}
     * @see Stream#filter(Predicate) for more information regarding this method
     * @see #filter(Predicate) for more information regarding this method
     */
    default FunctionalStream<T> removeAll(Predicate<? super T> filter) {
        return filter(t -> !filter.test(t));
    }

    /**
     * Retain anything in this {@link FunctionalStream} that matched
     * the given {@link Predicate}. Drops every element not matching it.
     *
     * @param filter the {@link Predicate} to use
     * @return the new {@link FunctionalStream}
     * @see Stream#filter(Predicate) for more information regarding this method
     * @see #filter(Predicate) for more information regarding this method
     */
    default FunctionalStream<T> retainAll(Predicate<? super T> filter) {
        return filter(filter);
    }

    /**
     * Remove any element that is null.
     *
     * @return the new {@link FunctionalStream}
     * @see Stream#filter(Predicate) for more information regarding this method
     * @see #filter(Predicate) for more information regarding this method
     */
    default FunctionalStream<T> removeNull() {
        return filter(Objects::nonNull);
    }

    /**
     * Retain every unique element of this {@link FunctionalStream}.
     * It uses a {@link HashSet} to check if the current element at
     * hand was already seen or not. Therefore this implementation
     * needs O(n) memory, where n is the size of the {@link FunctionalStream}.
     *
     * @return the new {@link FunctionalStream}
     * @see Stream#distinct() for more information regarding this method
     * @see #filter(Predicate) for more information regarding this method
     */
    default FunctionalStream<T> distinct() {
        Set<T> set = new HashSet<>();
        return filter(set::add);
    }

    /**
     * Unwrap a value that can be an {@link Optional} to the containing
     * value by filtering on {@link Optional#isPresent()} and than
     * calling {@link Optional#get()}. To get the {@link Optional} in
     * the first place on every element of this {@link FunctionalStream}
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
     * Retain any element in this {@link FunctionalStream} that is not
     * {@code null} and of type 'type'.
     *
     * @param <K> the new type of the {@link FunctionalStream}
     * @param type the {@link Class} type to retain in the {@link FunctionalStream}
     * @return the new {@link FunctionalStream}
     */
    default <K> FunctionalStream<K> ofType(@NonNull Class<K> type) {
        return removeNull().filter(t -> type.isAssignableFrom(t.getClass())).map(type::cast);
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
     * call one specific thing on every element of the {@link FunctionalStream}.
     *
     * @param consumer the {@link Consumer} to call
     * @return the new {@link FunctionalStream}
     * @see Stream#peek(Consumer) for more information regarding this method
     */
    FunctionalStream<T> peek(Consumer<? super T> consumer);

    // TODO: JavaDoc
    default FunctionalStream<T> higherOrderPeek(HigherOrderConsumer<T> higherOrderFilter) {
        return peek(t -> higherOrderFilter.apply(t).accept(t));
    }

    // TODO: JavaDoc
    default FunctionalStream<T> higherOrderPeekWithPrevious(T identity, HigherOrderConsumer<T> higherOrderFilter) {
        AtomicReference<T> current = new AtomicReference<>(identity);
        return peek(t -> higherOrderFilter.apply(current.getAndSet(t)).accept(t));
    }

    // TODO: JavaDoc
    default FunctionalStream<T> higherOrderPeekAndPrevious(T identity, BiHigherOrderConsumer<T> higherOrderFilter) {
        AtomicReference<T> current = new AtomicReference<>(identity);
        return peek(t -> higherOrderFilter.apply(current.getAndSet(t), t).accept(t));
    }

    /**
     * This can be used to debug the {@link FunctionalStream} and get
     * every element in a {@link Collection}.
     *
     * @param collection the {@link Collection} to put every element into
     * @return the new {@link FunctionalStream}
     */
    default FunctionalStream<T> peekResult(Collection<T> collection) {
        return peek(collection::add);
    }

    /**
     * Limit this {@link FunctionalStream} to n elements and retain
     * only those.
     *
     * @param count the retaining count
     * @return the new {@link FunctionalStream}
     * @see Stream#limit(long) for more information regarding this method
     */
    default FunctionalStream<T> limit(long count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative");
        }
        AtomicLong current = new AtomicLong(0L);
        return filter(t -> current.getAndIncrement() < count);
    }

    /**
     * Skip n elements of this {@link FunctionalStream} and retain
     * any after the skipped elements.
     *
     * @param count the skipping count
     * @return the new {@link FunctionalStream}
     * @see Stream#skip(long) for more information regarding this method
     */
    default FunctionalStream<T> skip(long count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative");
        }
        AtomicLong current = new AtomicLong(0L);
        return filter(t -> current.getAndIncrement() >= count);
    }

    /**
     * Keep a portion of this {@link FunctionalStream} by skipping
     * n elements and retaining n elements afterwards. This can be
     * used to implement paging.
     *
     * @param from which element should be the first in the {@link FunctionalStream}
     * @param to which element should be the last in the {@link FunctionalStream}
     * @return the new {@link FunctionalStream}
     */
    default FunctionalStream<T> keep(long from, long to) {
        if (to < from) {
            throw new IllegalArgumentException("From is Smaller than to");
        }
        return skip(from).limit(to - from);
    }

    /**
     * @return a {@link Stream} of this {@link FunctionalStream}.
     */
    Stream<T> toStream();

    /**
     * Map any element of this {@link FunctionalStream} by some {@link Function}
     * and ignore if any {@link Throwable} gets thrown. It will retain
     * every element, that mapped without an {@link Exception}.
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
     * Map any element of this {@link FunctionalStream} by some {@link Function}
     * and ignore if any {@link Throwable} gets thrown. It will retain
     * every element, that mapped without an {@link Exception}.
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
     * and return one combined {@link FunctionalStream} containing every element
     * of both {@link FunctionalStream}. This operation is optional and can combine
     * both {@link FunctionalStream} in any way, preferably this {@link FunctionalStream}
     * before the other. If this {@link FunctionalStream} is empty the zip method
     * must be evaluated and produce a {@link FunctionalStream} of the elements of
     * the inputted {@link FunctionalStream}.
     *
     * @param other the {@link FunctionalStream} to zip with
     * @return the new {@link FunctionalStream}
     */
    default FunctionalStream<T> concat(FunctionalStream<T> other) {
        throw new UnsupportedOperationException();
    }

    // Terminating methods

    /**
     * Terminate this {@link FunctionalStream} and apply the
     * given {@link Consumer} to every element left in the
     * {@link FunctionalStream}.
     *
     * @param consumer the {@link Consumer} to use
     * @see Stream#forEach(Consumer) for more information regarding this method
     */
    void forEach(Consumer<? super T> consumer);

    // TODO: JavaDoc
    default void higherOrderForEach(HigherOrderConsumer<T> higherOrderFilter) {
        forEach(t -> higherOrderFilter.apply(t).accept(t));
    }

    // TODO: JavaDoc
    default void higherOrderForEachWithPrevious(T identity, HigherOrderConsumer<T> higherOrderFilter) {
        AtomicReference<T> current = new AtomicReference<>(identity);
        forEach(t -> higherOrderFilter.apply(current.getAndSet(t)).accept(t));
    }

    // TODO: JavaDoc
    default void higherOrderForEachAndPrevious(T identity, BiHigherOrderConsumer<T> higherOrderFilter) {
        AtomicReference<T> current = new AtomicReference<>(identity);
        forEach(t -> higherOrderFilter.apply(current.getAndSet(t), t).accept(t));
    }

    /**
     * Terminate this {@link FunctionalStream} and collect
     * every element left in this {@link FunctionalStream}
     * into a {@link List}.
     *
     * @return the {@link List} of elements
     */
    default List<T> toList() {
        return collect(Collectors.toList());
    }

    /**
     * Terminate this {@link FunctionalStream} and collect
     * every element left in this {@link FunctionalStream}
     * into a {@link Set}.
     *
     * @return the {@link Set} of elements
     */
    default Set<T> toSet() {
        return collect(Collectors.toSet());
    }

    /**
     * Terminate this {@link FunctionalStream} and collect
     * every element left in this {@link FunctionalStream}
     * into a {@link String} with a specific delimiter.
     *
     * @param delimiter the delimiter to use
     * @return the {@link String} of every element joined by the delimiter
     */
    default String joining(String delimiter) {
        return map(Objects::toString).collect(Collectors.joining(delimiter));
    }

    /**
     * Terminate and evaluate every statement of this {@link FunctionalStream}
     * without any return value.
     */
    default void eval() {
        forEach(t -> {
        });
    }

    /**
     * Terminate this {@link FunctionalStream} and
     * check if one element can be found that qualifies to
     * the {@link Predicate} given. The {@link Predicate}
     * does not need to be evaluated on every element to
     * determine the output of this call.
     *
     * @param predicate the {@link Predicate} to use
     * @return if any element matched the {@link Predicate}
     * @see Stream#anyMatch(Predicate) for more information regarding this method
     */
    boolean anyMatch(Predicate<? super T> predicate);

    /**
     * Terminate this {@link FunctionalStream} and
     * check if every element qualifies to the
     * {@link Predicate} given. The {@link Predicate}
     * does not need to be evaluated on every element
     * to determine the output of this call.
     *
     * @param predicate the {@link Predicate} to use
     * @return if every element matched the {@link Predicate}
     * @see Stream#allMatch(Predicate) for more information regarding this method
     */
    boolean allMatch(Predicate<? super T> predicate);

    /**
     * Terminate this {@link FunctionalStream} and
     * check if no element qualified to the
     * {@link Predicate} given. The {@link Predicate}
     * does not need to be evaluated on every element
     * to determine the output of this call.
     *
     * @param predicate the {@link Predicate} to use
     * @return if no element matched the {@link Predicate}
     * @see Stream#noneMatch(Predicate) for more information regarding this method
     */
    boolean noneMatch(Predicate<? super T> predicate);

    /**
     * Terminate this {@link FunctionalStream} and
     * count the elements left in this
     * {@link FunctionalStream}.
     *
     * @return the element count of this {@link FunctionalStream}
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
     * the first element in this {@link FunctionalStream}.
     * If this {@link FunctionalStream} is empty
     * {@link Optional#empty()} gets returned.
     *
     * @return the first element of this {@link FunctionalStream} or none.
     * @see Stream#findFirst() for more information regarding this method
     */
    Optional<T> findFirst();

    /**
     * Terminate this {@link FunctionalStream} and return
     * the first element in this {@link FunctionalStream}
     * that is also matich the given {@link Predicate}.
     * If this {@link FunctionalStream} is empty
     * {@link Optional#empty()} gets returned.
     *
     * @param predicate the {@link Predicate} to test with
     * @return the first element of this {@link FunctionalStream} or none.
     * @see Stream#findFirst() for more information regarding this method
     */
    default Optional<T> findFirst(Predicate<T> predicate) {
        return filter(predicate).findFirst();
    }

    /**
     * Terminate this {@link FunctionalStream} and return
     * the smallest element determined by the {@link Comparator}
     * given. If this {@link FunctionalStream} is empty
     * {@link Optional#empty()} gets returned.
     *
     * @param comparator the {@link Comparator} to compare the elements
     * @return the smallest element of this {@link FunctionalStream} or none.
     * @see Stream#min(Comparator) for more information regarding this method
     */
    default Optional<T> min(Comparator<T> comparator) {
        AtomicReference<Optional<T>> result = new AtomicReference<>(Optional.empty());
        forEach(t -> {
            Optional<T> current = result.get();
            if (!current.isPresent()) {
                result.set(Optional.of(t));
                return;
            }
            if (comparator.compare(current.get(), t) > 0) {
                result.set(Optional.of(t));
            }
        });
        return result.get();
    }

    /**
     * Terminate this {@link FunctionalStream} and return
     * the smallest element determined by natural ordering.
     * If this {@link FunctionalStream} is empty {@link Optional#empty()}
     * gets returned. A {@link ClassCastException} can be thrown
     * when the type 'T' is not a {@link Comparable}.
     *
     * @return the smallest element of this {@link FunctionalStream} or none.
     * @see Stream#min(Comparator) for more information regarding this method
     */
    default Optional<T> min() {
        AtomicReference<Optional<T>> result = new AtomicReference<>(Optional.empty());
        forEach(t -> {
            Optional<T> current = result.get();
            if (!current.isPresent()) {
                result.set(Optional.of(t));
                return;
            }
            if (((Comparable<T>) current.get()).compareTo(t) > 0) {
                result.set(Optional.of(t));
            }
        });
        return result.get();
    }

    /**
     * Terminate this {@link FunctionalStream} and return
     * the biggest element determined by the {@link Comparator}
     * given. If this {@link FunctionalStream} is empty
     * {@link Optional#empty()} gets returned.
     *
     * @param comparator the {@link Comparator} to compare the elements
     * @return the biggest element of this {@link FunctionalStream} or none.
     * @see Stream#max(Comparator) for more information regarding this method
     */
    default Optional<T> max(Comparator<T> comparator) {
        AtomicReference<Optional<T>> result = new AtomicReference<>(Optional.empty());
        forEach(t -> {
            Optional<T> current = result.get();
            if (!current.isPresent()) {
                result.set(Optional.of(t));
                return;
            }
            if (comparator.compare(current.get(), t) < 0) {
                result.set(Optional.of(t));
            }
        });
        return result.get();
    }

    /**
     * Terminate this {@link FunctionalStream} and return
     * the biggest element determined by natural ordering.
     * If this {@link FunctionalStream} is empty {@link Optional#empty()}
     * gets returned. A {@link ClassCastException} can be thrown
     * when the type 'T' is not a {@link Comparable}.
     *
     * @return the smallest element of this {@link FunctionalStream} or none.
     * @see Stream#max(Comparator) for more information regarding this method
     */
    default Optional<T> max() {
        AtomicReference<Optional<T>> result = new AtomicReference<>(Optional.empty());
        forEach(t -> {
            Optional<T> current = result.get();
            if (!current.isPresent()) {
                result.set(Optional.of(t));
                return;
            }
            if (((Comparable<T>) current.get()).compareTo(t) < 0) {
                result.set(Optional.of(t));
            }
        });
        return result.get();
    }

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
    default <R, A> R collect(Collector<? super T, A, R> collector) {
        A container = collector.supplier().get();
        BiConsumer<A, ? super T> biConsumer = collector.accumulator();
        forEach(t -> biConsumer.accept(container, t));
        return collector.finisher().apply(container);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Float}. The {@link Function} will map every
     * element to type {@link Float}.
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
     * element to type {@link Integer}.
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
     * element to type {@link Double}.
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
     * element to type {@link Long}.
     *
     * @param longFunction the {@link Function} to produce the {@link Long}'s
     * @return the sum of every {@link Long}
     */
    default long longSum(Function<T, Long> longFunction) {
        return map(longFunction).reduce(0L, Long::sum);
    }

    /**
     * Terminate this {@link FunctionalStream} and return an
     * array of elements of the {@link FunctionalStream}.
     *
     * @param intFunction the array creation function
     * @return the array with every element
     * @see Stream#toArray(IntFunction) for more information regarding this method
     */
    default T[] toArray(IntFunction<T[]> intFunction) {
        List<T> list = toList();
        return list.toArray(intFunction.apply(list.size()));
    }

    /**
     * Terminate this {@link FunctionalStream} and return
     * a single return element determined by a given
     * 'identity' and an {@link BinaryOperator} to mutate
     * this initial 'identity' until every element is
     * used.
     *
     * @param identity the initial value
     * @param accumulator the accumulator to mutate the value
     * @return the single return element
     * @see Stream#reduce(Object, BinaryOperator) for more information regarding this method
     */
    default T reduce(T identity, BinaryOperator<T> accumulator) {
        AtomicReference<T> result = new AtomicReference<>(identity);
        forEach(t -> result.set(accumulator.apply(result.get(), t)));
        return result.get();
    }

    // API for internal use cases, like concat()

    /**
     * This is used as a common API for the implementation. Calling from outside should
     * not be done.
     *
     * @return if this stream has at least one element left
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
