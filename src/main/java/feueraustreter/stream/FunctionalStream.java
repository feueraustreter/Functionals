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

import feueraustreter.lambda.HigherOrderConsumer;
import feueraustreter.lambda.HigherOrderFunction;
import feueraustreter.lambda.HigherOrderPredicate;
import feueraustreter.lambda.ThrowableFunction;
import feueraustreter.tryfunction.Try;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface FunctionalStream<T> extends Iterable<T> {

    // Extension methods for lombok users, using `@ExtensionMethod(FunctionalStream.class)`

    static <K> FunctionalStream<K> functionalStream(Collection<K> collection) {
        return new FunctionalStreamImpl<>(collection.iterator());
    }

    static <K> FunctionalStream<K> fStream(Collection<K> collection) {
        return new FunctionalStreamImpl<>(collection.iterator());
    }

    static <K> FunctionalStream<K> functionalStream(Iterator<K> iterator) {
        return new FunctionalStreamImpl<>(iterator);
    }

    static <K> FunctionalStream<K> fStream(Iterator<K> iterator) {
        return new FunctionalStreamImpl<>(iterator);
    }

    static <K> FunctionalStream<K> functionalStream(Stream<K> stream) {
        return new FunctionalStreamImpl<>(stream.iterator());
    }

    static <K> FunctionalStream<K> fStream(Stream<K> stream) {
        return new FunctionalStreamImpl<>(stream.iterator());
    }

    static <K, V> FunctionalStream<Map.Entry<K, V>> functionalStream(Map<K, V> map) {
        return new FunctionalStreamImpl<>(map.entrySet().iterator());
    }

    static <K, V> FunctionalStream<Map.Entry<K, V>> fStream(Map<K, V> map) {
        return new FunctionalStreamImpl<>(map.entrySet().iterator());
    }

    // Creation methods

    /**
     * Create a {@link FunctionalStream} of an existing {@link Iterable}.
     *
     * @param <K>      the {@link FunctionalStream} type to use
     * @param iterable the {@link Iterable} ElementSource
     * @return the new {@link FunctionalStream}
     */
    static <K> FunctionalStream<K> of(Iterable<K> iterable) {
        return new FunctionalStreamImpl<>(iterable.iterator());
    }

    static <K> FunctionalStream<K> ofWithoutComodification(List<K> list) {
        return of(new Iterator<K>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return list.size() > index;
            }

            @Override
            public K next() {
                int current = index;
                index++;
                return list.get(current);
            }
        });
    }

    /**
     * Create a {@link FunctionalStream} of an existing {@link Iterator}.
     *
     * @param <K>      the {@link FunctionalStream} type to use
     * @param iterator the {@link Iterator} ElementSource
     * @return the new {@link FunctionalStream}
     */
    static <K> FunctionalStream<K> of(Iterator<K> iterator) {
        return new FunctionalStreamImpl<>(iterator);
    }

    /**
     * Create a {@link FunctionalStream} of an existing {@link Stream}.
     *
     * @param <K>    the {@link FunctionalStream} type to use
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
     * See {@link InputStream#read()}.
     *
     * @param inputStream the InputStream to use
     * @return the new {@link FunctionalStream}
     */
    static FunctionalStream<Integer> of(InputStream inputStream) {
        return new FunctionalStreamImpl<>(new Iterator<Integer>() {
            @Override
            @SneakyThrows
            public boolean hasNext() {
                return inputStream.available() > 0;
            }

            @Override
            @SneakyThrows
            public Integer next() {
                return inputStream.read();
            }
        });
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
     * @param <K>     the type of stream elements
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
     * @param <K>      the type of stream elements
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
     * @param <K>  the type of stream elements
     * @param seed the initial element
     * @param f    a function to be applied to the previous element to produce a new element
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
     * @param <K>     the type of stream elements
     * @param seed    the initial element
     * @param hasNext a predicate to apply to elements to determine when the stream must terminate.
     * @param next    a function to be applied to the previous element to produce a new element
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

    // TODO: JavaDoc
    static FunctionalStream<Integer> iterateInt(int initial, int end, int increment) {
        if (increment == 0) {
            throw new UnsupportedOperationException();
        }
        if (end > initial && increment < 0) {
            throw new UnsupportedOperationException();
        }
        return iterate(initial, l -> l < end, l -> l + increment);
    }

    // TODO: JavaDoc
    static FunctionalStream<Integer> iterateInt(int initial, int end) {
        return iterateInt(initial, end, 1);
    }

    // TODO: JavaDoc
    static FunctionalStream<Long> iterateLong(long initial, long end, long increment) {
        if (increment == 0) {
            throw new UnsupportedOperationException();
        }
        if (end > initial && increment < 0) {
            throw new UnsupportedOperationException();
        }
        return iterate(initial, l -> l < end, l -> l + increment);
    }

    // TODO: JavaDoc
    static FunctionalStream<Long> iterateLong(long initial, long end) {
        return iterateLong(initial, end, 1);
    }

    // TODO: JavaDoc
    static FunctionalStream<Float> iterateFloat(float initial, float end, float increment) {
        return iterate(initial, l -> l < end, l -> l + increment);
    }

    // TODO: JavaDoc
    static FunctionalStream<Double> iterateDouble(double initial, double end, double increment) {
        return iterate(initial, l -> l < end, l -> l + increment);
    }

    static <K> FunctionalStream<K> infinite() {
        return infinite(null);
    }

    static <K> FunctionalStream<K> infinite(K element) {
        return new FunctionalStreamImpl<>(new Iterator<K>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public K next() {
                return element;
            }
        });
    }

    static <K> FunctionalStream<K> random(Random random, Function<Random, K> generator) {
        return new FunctionalStreamImpl<>(new Iterator<K>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public K next() {
                return generator.apply(random);
            }
        });
    }

    static <K> FunctionalStream<K> random(Random random, Function<Random, K> generator, long count) {
        AtomicLong remaining = new AtomicLong(count);
        return new FunctionalStreamImpl<>(new Iterator<K>() {
            @Override
            public boolean hasNext() {
                return remaining.get() > 0;
            }

            @Override
            public K next() {
                if (remaining.getAndDecrement() <= 0) {
                    throw new NoSuchElementException();
                }
                return generator.apply(random);
            }
        });
    }

    /**
     * Returns an infinite sequential unordered stream where each element is
     * generated by the provided {@code Supplier}.  This is suitable for
     * generating constant streams, streams of random elements, etc.
     *
     * @param <K> the type of stream elements
     * @param s   the {@code Supplier} of generated elements
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

    // TODO: JavaDoc
    static <K> FunctionalStream<K> generate(LongPredicate hasNext, Supplier<? extends K> s) {
        AtomicLong index = new AtomicLong(0);
        return new FunctionalStreamImpl<>(new Iterator<K>() {
            @Override
            public boolean hasNext() {
                return hasNext.test(index.get());
            }

            @Override
            public K next() {
                index.incrementAndGet();
                return s.get();
            }
        });
    }

    // TODO: JavaDoc
    static <K> FunctionalStream<K> generate(BooleanSupplier hasNext, Supplier<? extends K> next) {
        return new FunctionalStreamImpl<>(new Iterator<K>() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public K next() {
                return next.get();
            }
        });
    }

    // Conversion methods

    /**
     * Convert this {@link FunctionalStream} of one type to another
     * by using the mapper {@link Function} provided. It will be applied
     * to every element of the current {@link FunctionalStream}.
     *
     * @param <K>    the new type of the {@link FunctionalStream}
     * @param mapper the mapper {@link Function} to use
     * @return the new {@link FunctionalStream}
     * @see Stream#map(Function) for more information regarding this method
     */
    <K> FunctionalStream<K> map(Function<? super T, K> mapper);

    // TODO: JavaDoc
    default <K> FunctionalStream<K> higherOrderMap(HigherOrderFunction<T, K> higherOrderMap) {
        return map(t -> higherOrderMap.apply(t).apply(t));
    }

    /**
     * Convert a {@link FunctionalStream} of {@link FunctionalStream}
     * to a {@link FunctionalStream} by applying every element of the
     * containing {@link FunctionalStream} to the new {@link FunctionalStream}.
     *
     * @param <K>    the new type of the {@link FunctionalStream}
     * @param mapper the mapper {@link Function} to use and get the {@link FunctionalStream} to get the data from
     * @return the new {@link FunctionalStream}
     * @see Stream#flatMap(Function) for more information regarding this method
     */
    <K> FunctionalStream<K> flatMap(Function<? super T, FunctionalStream<K>> mapper);

    // TODO: JavaDoc
    default <K> FunctionalStream<K> flatten(Function<? super T, FunctionalStream<K>> mapper) {
        return flatMap(mapper);
    }

    /**
     * Convert a {@link FunctionalStream} of Arrays to a {@link FunctionalStream}
     * by applying every element of the containing {@link FunctionalStream} to
     * the new {@link FunctionalStream}.
     *
     * @param <K>    the new type of the {@link FunctionalStream}
     * @param mapper the mapper {@link Function} to use and get the Array to get the data from
     * @return the new {@link FunctionalStream}
     * @see Stream#flatMap(Function) for more information regarding this method
     */
    default <K> FunctionalStream<K> flatArrayMap(Function<? super T, K[]> mapper) {
        return flatMap(t -> FunctionalStream.of(mapper.apply(t)));
    }

    // TODO: JavaDoc
    default <K> FunctionalStream<K> flatStreamMap(Function<? super T, Stream<K>> mapper) {
        return flatMap(t -> FunctionalStream.of(mapper.apply(t)));
    }

    // TODO: JavaDoc
    default <K> FunctionalStream<K> flatIteratorMap(Function<? super T, Iterator<K>> mapper) {
        return flatMap(t -> FunctionalStream.of(mapper.apply(t)));
    }

    // TODO: JavaDoc
    default <K> FunctionalStream<K> flatIterableMap(Function<? super T, Iterable<K>> mapper) {
        return flatMap(t -> FunctionalStream.of(mapper.apply(t)));
    }

    // TODO: JavaDoc
    default <K> FunctionalStream<K> flatCollectionMap(Function<? super T, Collection<K>> mapper) {
        return flatMap(t -> FunctionalStream.of(mapper.apply(t)));
    }

    // TODO: JavaDoc
    default <K, V> FunctionalStream<Map.Entry<K, V>> flatMapMap(Function<? super T, Map<K, V>> mapper) {
        return flatMap(t -> FunctionalStream.of(mapper.apply(t)));
    }

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
        return map(t -> filter.test(t) ? mapper.apply(t) : t);
    }

    // TODO: JavaDoc
    default <K> FunctionalStream<K> forkingMap(Predicate<? super T> filter, Function<? super T, K> filteredMap, Function<? super T, K> unfilteredMap) {
        return map(t -> filter.test(t) ? filteredMap.apply(t) : unfilteredMap.apply(t));
    }

    // TODO: JavaDoc
    default <K, E extends Throwable> FunctionalStream<K> mapFilter(ThrowableFunction<T, E, K> throwableFunction) {
        return map(t -> Try.tryIt(() -> throwableFunction.apply(t))).filter(Try::successful).map(Try::getSuccess);
    }

    // TODO: JavaDoc
    default FunctionalStream<T> scan(BinaryOperator<T> mutator) {
        AtomicReference<T> currentValue = new AtomicReference<>(null);
        AtomicBoolean wasFirst = new AtomicBoolean(true);
        return map(t -> {
            if (wasFirst.get()) {
                wasFirst.set(false);
                currentValue.set(t);
                return currentValue.get();
            }
            T newValue = mutator.apply(currentValue.get(), t);
            currentValue.set(newValue);
            return newValue;
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
    default FunctionalStream<T> andFilter(Predicate<? super T>[] filters) {
        return filter(t -> Arrays.stream(filters).allMatch(f -> f.test(t)));
    }

    // TODO: JavaDoc
    default FunctionalStream<T> orFilter(Predicate<? super T>[] filters) {
        return filter(t -> Arrays.stream(filters).anyMatch(f -> f.test(t)));
    }

    // TODO: JavaDoc
    default FunctionalStream<T> filterIdentitySequences() {
        AtomicReference<T> current = new AtomicReference<>();
        return filter(t -> current.getAndSet(t) != t);
    }

    // TODO: JavaDoc
    default FunctionalStream<T> filterSequences() {
        AtomicReference<T> current = new AtomicReference<>();
        AtomicBoolean initial = new AtomicBoolean(true);
        return filter(t -> {
            if (t == null && initial.get()) return initial.getAndSet(false);
            if (current.get() == null && t == null) return false;
            if (current.get() != null && current.get().equals(t)) return false;
            current.set(t);
            return true;
        });
    }

    // TODO: JavaDoc
    default FunctionalStream<T> higherOrderFilter(HigherOrderPredicate<? super T> higherOrderFilter) {
        return filter(t -> higherOrderFilter.apply(t).test(t));
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
    default FunctionalStream<T> drop(Predicate<? super T> filter) {
        return filter(t -> !filter.test(t));
    }

    /**
     * Remove any element that is null.
     *
     * @return the new {@link FunctionalStream}
     * @see Stream#filter(Predicate) for more information regarding this method
     * @see #filter(Predicate) for more information regarding this method
     */
    default FunctionalStream<T> dropNull() {
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
        return distinct(set);
    }

    /**
     * Retain every unique element of this {@link FunctionalStream}.
     * It uses a {@link HashSet} to check if the current element at
     * hand was already seen or not. Therefore this implementation
     * needs O(n) memory, where n is the size of the {@link FunctionalStream}.
     *
     * @param distinctionSet the {@link Set} to use for checking if the current element at hand was already seen or not
     * @return the new {@link FunctionalStream}
     * @see Stream#distinct() for more information regarding this method
     * @see #filter(Predicate) for more information regarding this method
     */
    default FunctionalStream<T> distinct(Set<T> distinctionSet) {
        return filter(distinctionSet::add);
    }

    // TODO: JavaDoc
    default FunctionalStream<T> distinct(Consumer<Set<T>> resultConsumer) {
        Set<T> set = new HashSet<>();
        return distinct(set).onFinish(() -> resultConsumer.accept(set));
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
     * @param <K>          the new type of the {@link FunctionalStream}
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
     * @param <K>  the new type of the {@link FunctionalStream}
     * @param type the {@link Class} type to retain in the {@link FunctionalStream}
     * @return the new {@link FunctionalStream}
     */
    default <K> FunctionalStream<K> ofType(@NonNull Class<K> type) {
        return filter(t -> t == null || type.isAssignableFrom(t.getClass())).map(type::cast);
    }

    /**
     * Retain any element in this {@link FunctionalStream} that is not
     * {@code null} and of type 'type'.
     *
     * @param <K>  the new type of the {@link FunctionalStream}
     * @param type the {@link Class} type to retain in the {@link FunctionalStream}
     * @return the new {@link FunctionalStream}
     */
    default <K> FunctionalStream<K> as(@NonNull Class<K> type) {
        return ofType(type);
    }

    /**
     * Apply some kind of mapping to this {@link FunctionalStream} outside
     * this caller. It can be used by an API to template a specific mapping
     * and provide it as an method to use by the user.
     *
     * @param <K>             the new type of the {@link FunctionalStream}
     * @param tappingFunction the {@link Function} to convert from old to new {@link FunctionalStream}
     * @return the new {@link FunctionalStream}
     */
    default <K> FunctionalStream<K> tap(Function<FunctionalStream<? super T>, FunctionalStream<K>> tappingFunction) {
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
    default FunctionalStream<T> peek(Consumer<? super T> consumer) {
        return filter(t -> {
            consumer.accept(t);
            return true;
        });
    }

    // TODO: JavaDoc
    default FunctionalStream<T> each(Consumer<? super T> consumer) {
        return peek(consumer);
    }

    default FunctionalStream<T> peek(Consumer<? super T> consumer, Predicate<? super T> condition) {
        return filter(t -> {
            if (condition.test(t)) consumer.accept(t);
            return true;
        });
    }

    // TODO: JavaDoc
    default FunctionalStream<T> each(Consumer<? super T> consumer, Predicate<? super T> condition) {
        return peek(consumer, condition);
    }

    // TODO: JavaDoc
    default FunctionalStream<T> higherOrderPeek(HigherOrderConsumer<? super T> higherOrderPeek) {
        return peek(t -> higherOrderPeek.apply(t).accept(t));
    }

    // TODO: JavaDoc
    default FunctionalStream<T> higherOrderEach(HigherOrderConsumer<? super T> higherOrderEach) {
        return higherOrderPeek(higherOrderEach);
    }

    /**
     * This can be used to debug the {@link FunctionalStream} and get
     * every element in a {@link Collection}.
     *
     * @param collection the {@link Collection} to put every element into
     * @return the new {@link FunctionalStream}
     */
    default FunctionalStream<T> peek(Collection<? super T> collection) {
        return peek(collection::add);
    }

    // TODO: JavaDoc
    default FunctionalStream<T> each(Collection<? super T> collection) {
        return peek(collection);
    }

    // TODO: JavaDoc
    default FunctionalStream<T> inline(Runnable runnable) {
        return peek(ignored -> runnable.run());
    }

    // TODO: JavaDoc
    default FunctionalStream<T> each(Runnable runnable) {
        return inline(runnable);
    }

    // TODO: JavaDoc
    default FunctionalStream<T> inline(Runnable runnable, Predicate<? super T> condition) {
        return peek(ignored -> runnable.run(), condition);
    }

    // TODO: JavaDoc
    default FunctionalStream<T> each(Runnable runnable, Predicate<? super T> condition) {
        return inline(runnable, condition);
    }

    /**
     * Limit this {@link FunctionalStream} to n elements and retain
     * only those.
     *
     * @param count the retaining count
     * @return the new {@link FunctionalStream}
     * @see Stream#limit(long) for more information regarding this method
     */
    default FunctionalStream<T> take(long count) {
        return limit(count);
    }

    // TODO: JavaDoc
    default FunctionalStream<T> takeWhile(Predicate<T> predicate) {
        AtomicBoolean skip = new AtomicBoolean(false);
        return filter(t -> {
            if (!skip.get()) {
                skip.set(!predicate.test(t));
            }
            return skip.get();
        });
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
        return filter(t -> {
            if (current.incrementAndGet() < count) {
                return true;
            }
            close();
            return true;
        });
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
        return filter(t -> current.incrementAndGet() > count);
    }

    // TODO: JavaDoc
    default FunctionalStream<T> skipWhile(Predicate<? super T> predicate) {
        AtomicBoolean skip = new AtomicBoolean(false);
        return filter(t -> {
            if (skip.get()) {
                skip.set(!predicate.test(t));
            }
            return skip.get();
        });
    }

    /**
     * Skip elements of this {@link FunctionalStream} until the
     * {@link Predicate} returns true.
     *
     * @param count the skipping count
     * @return the new {@link FunctionalStream}
     */
    default FunctionalStream<T> drop(long count) {
        return skip(count);
    }

    // TODO: JavaDoc
    default FunctionalStream<T> dropWhile(Predicate<? super T> predicate) {
        return skipWhile(predicate);
    }

    /**
     * Keep a portion of this {@link FunctionalStream} by skipping
     * n elements and retaining n elements afterwards. This can be
     * used to implement paging.
     *
     * @param from which element should be the first in the {@link FunctionalStream}
     * @param to   which element should be the last in the {@link FunctionalStream}
     * @return the new {@link FunctionalStream}
     */
    default FunctionalStream<T> keep(long from, long to) {
        if (to < from) {
            throw new IllegalArgumentException("from is smaller than to");
        }
        return skip(from).limit(to - from);
    }

    // TODO: JavaDoc
    default FunctionalStream<T> keepWhile(Predicate<T> predicate) {
        return dropWhile(predicate).takeWhile(predicate.negate());
    }

    /**
     * @return a {@link Stream} of this {@link FunctionalStream}.
     * @implNote This operation is optional.
     */
    default Stream<T> toStream() {
        throw new UnsupportedOperationException();
    }

    /**
     * Map any element of this {@link FunctionalStream} by some {@link Function}
     * and ignore if any {@link Throwable} gets thrown. It will retain
     * every element, that mapped without an {@link Exception}.
     *
     * @param <K>         the new type of the {@link FunctionalStream}
     * @param <E>         the {@link Throwable} type of the applied {@link Function}
     * @param tryFunction the Function to apply.
     * @return the new {@link FunctionalStream}
     */
    default <K, E extends Throwable> FunctionalStream<K> tryIt(Function<T, Try<K, E>> tryFunction) {
        return map(tryFunction).filter(Try::successful).map(Try::getSuccess);
    }

    /**
     * Combine this {@link FunctionalStream} with another {@link FunctionalStream}
     * and return one combined {@link FunctionalStream} containing every element
     * of both {@link FunctionalStream}.
     *
     * @param other the {@link FunctionalStream} to concat with
     * @return the new {@link FunctionalStream}
     * @implSpec This operation should not terminate the {@link FunctionalStream} in
     * any way and work with any other operation done after this.
     * @implNote This operation is optional and can combine both {@link FunctionalStream}
     * in any way, preferably this {@link FunctionalStream} before the {@code other}.
     * If this {@link FunctionalStream} is empty the concat method must be evaluated and
     * produce a {@link FunctionalStream} of the elements of the inputted {@link FunctionalStream}.
     */
    default FunctionalStream<T> concat(FunctionalStream<T> other) {
        return of(this, other).flatMap(ts -> ts);
    }

    /**
     * Calls {@link #concat(FunctionalStream)} after {@link #of(Stream)} with the given {@link Stream}.
     *
     * @param other the other {@link Stream}
     * @return the new {@link FunctionalStream}
     */
    default FunctionalStream<T> concat(Stream<T> other) {
        return concat(of(other));
    }

    /**
     * Calls {@link #concat(FunctionalStream)} after {@link #of(Iterable)} with the given {@link Iterable}.
     *
     * @param other the other {@link Iterable}
     * @return the new {@link FunctionalStream}
     */
    default FunctionalStream<T> concat(Iterable<T> other) {
        return concat(of(other));
    }

    /**
     * Calls {@link #concat(FunctionalStream)} after {@link #of(Iterator)} with the given {@link Iterator}.
     *
     * @param other the other {@link Iterator}
     * @return the new {@link FunctionalStream}
     */
    default FunctionalStream<T> concat(Iterator<T> other) {
        return concat(of(other));
    }

    // TODO: JavaDoc
    default FunctionalStream<T> insert(Consumer<Sink<T>> sink) {
        LinkedList<T> list = new LinkedList<>();
        sink.accept(list::add);
        return concat(ofWithoutComodification(list));
    }

    // TODO: JavaDoc
    default FunctionalStream<T> insert(Consumer<Sink<T>> sink, Predicate<? super T> filter) {
        AtomicReference<Sink<T>> sinkAtomicReference = new AtomicReference<>();
        FunctionalStream<T> insertedStream = insert(sinkAtomicReference::set);
        sink.accept(t -> {
            if (filter.test(t)) sinkAtomicReference.get().accept(t);
        });
        return insertedStream;
    }

    // TODO: JavaDoc
    default FunctionalStream<T> duplicate() {
        return duplicate(1);
    }

    // TODO: JavaDoc
    default FunctionalStream<T> duplicate(long duplications) {
        return flatMap(t -> generate(l -> l <= duplications, () -> t));
    }

    // TODO: JavaDoc
    default FunctionalStream<T> onClose(Runnable runnable) {
        throw new UnsupportedOperationException();
    }

    // TODO: JavaDoc
    default FunctionalStream<T> onFinish(Runnable runnable) {
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
    default void higherOrderForEach(HigherOrderConsumer<? super T> higherOrderForEach) {
        forEach(t -> higherOrderForEach.apply(t).accept(t));
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
        return map(Object::toString).collect(Collectors.joining(delimiter));
    }

    /**
     * Terminate this {@link FunctionalStream} and collect it
     * to a specific type by using a {@link Collector}. Some
     * common {@link Collector}'s can be found in the
     * {@link Collectors} class.
     *
     * @param <R>       the type of the result
     * @param <A>       the intermediate accumulation type of the {@link Collector}
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
     * Terminate and evaluate every statement of this {@link FunctionalStream}
     * without any return value.
     */
    default void eval() {
        forEach(t -> {
        });
    }

    /**
     * Terminate and evaluate this {@link FunctionalStream} but stop on the first
     * result found. This is the equivalent of calling {@link #findFirst()} and
     * ignoring the return value.
     */
    default void evalFirst() {
        findFirst();
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
    default boolean anyMatch(Predicate<? super T> predicate) {
        return !noneMatch(predicate);
    }

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
    default boolean allMatch(Predicate<? super T> predicate) {
        return noneMatch(predicate.negate());
    }

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
    default boolean noneMatch(Predicate<? super T> predicate) {
        AtomicBoolean found = new AtomicBoolean(true);
        filter(predicate).forEach(t -> {
            found.set(false);
            close();
        });
        return found.get();
    }

    /**
     * Terminate this {@link FunctionalStream} and
     * count the elements left in this
     * {@link FunctionalStream}.
     *
     * @return the element count of this {@link FunctionalStream}
     * @see Stream#count() for more information regarding this method
     */
    default long count() {
        AtomicLong result = new AtomicLong();
        forEach(t -> result.incrementAndGet());
        return result.get();
    }

    /**
     * Terminate this {@link FunctionalStream} without
     * evaluating the rest.
     *
     * @implSpec When closing a {@link FunctionalStream} in a terminating
     * operation the {@link FunctionalStream} should evaluate no further elements
     * and return what it has right now.
     * @implNote When you terminate a terminated {@link FunctionalStream} an
     * {@link Exception} should be thrown. Any subsequent calls to {@code #close()}
     * should be ignored. This terminating behaviour is crucial to some default
     * implementations.
     * @see Stream#close() for more information regarding this method
     */
    void close();

    // TODO: JavaDoc
    boolean isClosed();

    /**
     * Terminate this {@link FunctionalStream} and return
     * the first element in this {@link FunctionalStream}.
     * If this {@link FunctionalStream} is empty or the
     * Element is {@code null} {@link Optional#empty()}
     * gets returned.
     *
     * @return the first element of this {@link FunctionalStream} or none.
     * @see Stream#findFirst() for more information regarding this method
     */
    default Optional<T> findFirst() {
        AtomicReference<Optional<T>> result = new AtomicReference<>(Optional.empty());
        forEach(t -> {
            result.set(Optional.ofNullable(t));
            close();
        });
        return result.get();
    }

    /**
     * Terminate this {@link FunctionalStream} and return
     * the first element in this {@link FunctionalStream}
     * that is also matching the given {@link Predicate}.
     * If this {@link FunctionalStream} is empty or the
     * Element is {@code null} {@link Optional#empty()}
     * gets returned.
     *
     * @param predicate the {@link Predicate} to test with
     * @return the first element of this {@link FunctionalStream} or none.
     * @see Stream#findFirst() for more information regarding this method
     */
    default Optional<T> findFirst(Predicate<? super T> predicate) {
        return filter(predicate).findFirst();
    }

    /**
     * Terminate this {@link FunctionalStream} and return
     * the last element in this {@link FunctionalStream}.
     * If this {@link FunctionalStream} is empty or the
     * Element is {@code null} {@link Optional#empty()}
     * gets returned.
     *
     * @return the first element of this {@link FunctionalStream} or none.
     * @see Stream#findFirst() for more information regarding this method
     */
    default Optional<T> findLast() {
        AtomicReference<Optional<T>> result = new AtomicReference<>(Optional.empty());
        forEach(t -> result.set(Optional.ofNullable(t)));
        return result.get();
    }

    /**
     * Terminate this {@link FunctionalStream} and return
     * the last element in this {@link FunctionalStream}
     * that is also matching the given {@link Predicate}.
     * If this {@link FunctionalStream} is empty or the
     * Element is {@code null} {@link Optional#empty()}
     * gets returned.
     *
     * @param predicate the {@link Predicate} to test with
     * @return the first element of this {@link FunctionalStream} or none.
     * @see Stream#findFirst() for more information regarding this method
     */
    default Optional<T> findLast(Predicate<? super T> predicate) {
        return filter(predicate).findLast();
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
    default Optional<T> min(Comparator<? super T> comparator) {
        AtomicReference<Optional<T>> result = new AtomicReference<>(Optional.empty());
        forEach(t -> {
            Optional<T> current = result.get();
            if (!current.isPresent()) {
                result.set(Optional.ofNullable(t));
                return;
            }
            if (comparator.compare(current.get(), t) > 0) {
                result.set(Optional.ofNullable(t));
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
        return min((o1, o2) -> ((Comparable<? super T>) o1).compareTo(o2));
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
    default Optional<T> max(Comparator<? super T> comparator) {
        AtomicReference<Optional<T>> result = new AtomicReference<>(Optional.empty());
        forEach(t -> {
            Optional<T> current = result.get();
            if (!current.isPresent()) {
                result.set(Optional.ofNullable(t));
                return;
            }
            if (comparator.compare(current.get(), t) < 0) {
                result.set(Optional.ofNullable(t));
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
        return max((o1, o2) -> ((Comparable<? super T>) o1).compareTo(o2));
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

    // TODO: JavaDoc
    default float floatSum() {
        return floatSum(Float.class::cast);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Float}. The {@link Function} will map every
     * element to type {@link Float}.
     *
     * @param floatFunction the {@link Function} to produce the {@link Float}'s
     * @param identity      the identity value for the reduction
     * @return the sum of every {@link Float}
     */
    default float floatSum(Function<T, Float> floatFunction, float identity) {
        return map(floatFunction).reduce(identity, Float::sum);
    }

    // TODO: JavaDoc
    default float floatSum(float identity) {
        return floatSum(Float.class::cast, identity);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Float}. The {@link Function} will map every
     * element to type {@link Float}.
     *
     * @param floatFunction the {@link Function} to produce the {@link Float}'s
     * @return the multiplication of every {@link Float}
     */
    default float floatMultiplication(Function<T, Float> floatFunction) {
        return map(floatFunction).reduce(1.0F, (a, b) -> a * b);
    }

    // TODO: JavaDoc
    default float floatMultiplication() {
        return floatMultiplication(Float.class::cast);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Float}. The {@link Function} will map every
     * element to type {@link Float}.
     *
     * @param floatFunction the {@link Function} to produce the {@link Float}'s
     * @param identity      the identity value for the reduction
     * @return the multiplication of every {@link Float}
     */
    default float floatMultiplication(Function<T, Float> floatFunction, float identity) {
        return map(floatFunction).reduce(identity, (a, b) -> a * b);
    }

    // TODO: JavaDoc
    default float floatMultiplication(float identity) {
        return floatMultiplication(Float.class::cast, identity);
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

    // TODO: JavaDoc
    default int integerSum() {
        return integerSum(Integer.class::cast);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Integer}. The {@link Function} will map every
     * element to type {@link Integer}.
     *
     * @param integerFunction the {@link Function} to produce the {@link Integer}'s
     * @param identity        the identity value for the reduction
     * @return the sum of every {@link Integer}
     */
    default int integerSum(Function<T, Integer> integerFunction, int identity) {
        return map(integerFunction).reduce(identity, Integer::sum);
    }

    // TODO: JavaDoc
    default int integerSum(int identity) {
        return integerSum(Integer.class::cast, identity);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Integer}. The {@link Function} will map every
     * element to type {@link Integer}.
     *
     * @param integerFunction the {@link Function} to produce the {@link Integer}'s
     * @return the multiplication of every {@link Integer}
     */
    default int integerMultiplication(Function<T, Integer> integerFunction) {
        return map(integerFunction).reduce(1, (a, b) -> a * b);
    }

    // TODO: JavaDoc
    default int integerMultiplication() {
        return integerMultiplication(Integer.class::cast);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Integer}. The {@link Function} will map every
     * element to type {@link Integer}.
     *
     * @param integerFunction the {@link Function} to produce the {@link Integer}'s
     * @param identity        the identity value for the reduction
     * @return the multiplication of every {@link Integer}
     */
    default int integerMultiplication(Function<T, Integer> integerFunction, int identity) {
        return map(integerFunction).reduce(identity, (a, b) -> a * b);
    }

    // TODO: JavaDoc
    default int integerMultiplication(int identity) {
        return integerMultiplication(Integer.class::cast, identity);
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

    // TODO: JavaDoc
    default double doubleSum() {
        return doubleSum(Double.class::cast);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Double}. The {@link Function} will map every
     * element to type {@link Double}.
     *
     * @param doubleFunction the {@link Function} to produce the {@link Double}'s
     * @param identity       the identity value for the reduction
     * @return the sum of every {@link Double}
     */
    default double doubleSum(Function<T, Double> doubleFunction, double identity) {
        return map(doubleFunction).reduce(identity, Double::sum);
    }

    // TODO: JavaDoc
    default double doubleSum(double identity) {
        return doubleSum(Double.class::cast, identity);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Double}. The {@link Function} will map every
     * element to type {@link Double}.
     *
     * @param doubleFunction the {@link Function} to produce the {@link Double}'s
     * @return the multiplication of every {@link Double}
     */
    default double doubleMultiplication(Function<T, Double> doubleFunction) {
        return map(doubleFunction).reduce(0.0D, (a, b) -> a * b);
    }

    // TODO: JavaDoc
    default double doubleMultiplication() {
        return doubleMultiplication(Double.class::cast);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Double}. The {@link Function} will map every
     * element to type {@link Double}.
     *
     * @param doubleFunction the {@link Function} to produce the {@link Double}'s
     * @param identity       the identity value for the reduction
     * @return the multiplication of every {@link Double}
     */
    default double doubleMultiplication(Function<T, Double> doubleFunction, double identity) {
        return map(doubleFunction).reduce(identity, (a, b) -> a * b);
    }

    // TODO: JavaDoc
    default double doubleMultiplication(double identity) {
        return doubleMultiplication(Double.class::cast, identity);
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

    // TODO: JavaDoc
    default long longSum() {
        return longSum(Long.class::cast);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Long}. The {@link Function} will map every
     * element to type {@link Long}.
     *
     * @param longFunction the {@link Function} to produce the {@link Long}'s
     * @param identity     the identity value for the reduction
     * @return the sum of every {@link Long}
     */
    default long longSum(Function<T, Long> longFunction, long identity) {
        return map(longFunction).reduce(identity, Long::sum);
    }

    // TODO: JavaDoc
    default long longSum(long identity) {
        return longSum(Long.class::cast, identity);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Long}. The {@link Function} will map every
     * element to type {@link Long}.
     *
     * @param longFunction the {@link Function} to produce the {@link Long}'s
     * @return the multiplication of every {@link Long}
     */
    default long longMultiplication(Function<T, Long> longFunction) {
        return map(longFunction).reduce(1L, (a, b) -> a * b);
    }

    // TODO: JavaDoc
    default long longMultiplication() {
        return longMultiplication(Long.class::cast);
    }

    /**
     * Terminate and reduce this {@link FunctionalStream} to
     * a {@link Long}. The {@link Function} will map every
     * element to type {@link Long}.
     *
     * @param longFunction the {@link Function} to produce the {@link Long}'s
     * @param identity     the identity value for the reduction
     * @return the multiplication of every {@link Long}
     */
    default long longMultiplication(Function<T, Long> longFunction, long identity) {
        return map(longFunction).reduce(identity, (a, b) -> a * b);
    }

    // TODO: JavaDoc
    default long longMultiplication(long identity) {
        return longMultiplication(Long.class::cast, identity);
    }

    // TODO: JavaDoc
    default T reduce(BinaryOperator<T> accumulator) {
        AtomicReference<T> ref = new AtomicReference<>(null);
        AtomicBoolean wasFirst = new AtomicBoolean(true);
        forEach(t -> {
            if (wasFirst.get()) {
                ref.set(t);
            } else {
                ref.set(accumulator.apply(ref.get(), t));
            }
        });
        return ref.get();
    }

    /**
     * Terminate this {@link FunctionalStream} and return
     * a single return element determined by a given
     * 'identity' and an {@link BinaryOperator} to mutate
     * this initial 'identity' until every element is
     * used.
     *
     * @param identity    the initial value
     * @param accumulator the accumulator to mutate the value
     * @return the single return element
     * @see Stream#reduce(Object, BinaryOperator) for more information regarding this method
     */
    default T reduce(T identity, BinaryOperator<T> accumulator) {
        AtomicReference<T> result = new AtomicReference<>(identity);
        forEach(t -> result.set(accumulator.apply(result.get(), t)));
        return result.get();
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
     *
     * @return the next element of the underlying stream
     */
    default T nextElement() {
        throw new UnsupportedOperationException();
    }
}
