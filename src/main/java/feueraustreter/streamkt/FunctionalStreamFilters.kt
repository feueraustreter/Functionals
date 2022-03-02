package feueraustreter.streamkt

import java.util.Optional
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

fun <T> FunctionalStream<T>.indexFilter(filter: ((Long) -> Boolean)): FunctionalStream<T> {
    val atomicLong = AtomicLong()
    return filter { filter(atomicLong.getAndIncrement()) }
}

fun <T> FunctionalStream<T>.andFilter(vararg filters: (T) -> Boolean): FunctionalStream<T> {
    return filter { value ->
        filters.all { it(value) }
    }
}

fun <T> FunctionalStream<T>.orFilter(vararg filters: (T) -> Boolean): FunctionalStream<T> {
    return filter { value ->
        filters.any { it(value) }
    }
}

fun <T> FunctionalStream<T>.filterIdentitySequences(): FunctionalStream<T> {
    val current = AtomicReference<T>()
    return filter { current.getAndSet(it) !== it }
}

fun <T> FunctionalStream<T>.filterSequences(): FunctionalStream<T> {
    val current = AtomicReference<T>()
    val initial = AtomicBoolean(true)
    return filter {
        if (it == null && initial.get()) return@filter initial.getAndSet(false)
        if (current.get() == null && it == null) return@filter false
        if (current.get() != null && current.get() == it) return@filter false
        current.set(it)
        return@filter true
    }
}

fun <T> FunctionalStream<T>.higherOrderFilter(filter: ((T) -> ((T) -> Boolean))): FunctionalStream<T> {
    return filter { value ->
        filter(value)(value)
    }
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
fun <T> FunctionalStream<T>.drop(filter: (T) -> Boolean): FunctionalStream<T> {
    return filter { value ->
        !filter(value)
    }
}

/**
 * Remove any element that is null.
 *
 * @return the new {@link FunctionalStream}
 * @see Stream#filter(Predicate) for more information regarding this method
 * @see #filter(Predicate) for more information regarding this method
 */
fun <T> FunctionalStream<T?>.dropNulls(): FunctionalStream<T> {
    return drop { it == null } as FunctionalStream<T>
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
fun <T> FunctionalStream<T>.distinct(): FunctionalStream<T> {
    val set = mutableSetOf<T>()
    return distinct(set)
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
fun <T> FunctionalStream<T>.distinct(set: MutableSet<T>): FunctionalStream<T> {
    return filter { set.add(it) }
}

fun <T> FunctionalStream<T>.distinct(resultConsumer: ((MutableSet<T>) -> Unit)): FunctionalStream<T> {
    val set = mutableSetOf<T>()
    return filter { set.add(it) }.onFinish { resultConsumer(set) }
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
 * @return the new {@link FunctionalStream}
 */
fun <T> FunctionalStream<Optional<T>>.unwrap(): FunctionalStream<T> {
    return filter { it.isPresent }.map { it.get() }
}

/**
 * Retain any element in this {@link FunctionalStream} that is not
 * {@code null} and of type 'type'.
 *
 * @param <K>  the new type of the {@link FunctionalStream}
 * @param type the {@link Class} type to retain in the {@link FunctionalStream}
 * @return the new {@link FunctionalStream}
 */
fun <T, K> FunctionalStream<T>.ofType(clazz: Class<K>): FunctionalStream<K> {
    return filter { clazz.isInstance(it) }.map { clazz.cast(it) }
}
