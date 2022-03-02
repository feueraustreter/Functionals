package feueraustreter.streamkt

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Limit this {@link FunctionalStream} to n elements and retain
 * only those.
 *
 * @param count the retaining count
 * @return the new {@link FunctionalStream}
 * @see Stream#limit(long) for more information regarding this method
 */
fun <T> FunctionalStream<T>.take(count: Long): FunctionalStream<T> =
    limit(count)

fun <T> FunctionalStream<T>.takeWhile(predicate: (T) -> Boolean): FunctionalStream<T> {
    val skip = AtomicBoolean(true)
    return filter {
        if (skip.get()) {
            skip.set(predicate(it))
        }
        skip.get()
    }
}

/**
 * Limit this {@link FunctionalStream} to n elements and retain
 * only those.
 *
 * @param count the retaining count
 * @return the new {@link FunctionalStream}
 * @see Stream#limit(long) for more information regarding this method
 */
fun <T> FunctionalStream<T>.limit(count: Long): FunctionalStream<T> {
    if (count <= 0) {
        throw IllegalArgumentException("count must be greater than 0")
    }
    val current = AtomicLong(0L)
    return filter {
        if (current.incrementAndGet() < count) {
            return@filter true
        }
        close()
        true
    }
}

/**
 * Skip n elements of this {@link FunctionalStream} and retain
 * any after the skipped elements.
 *
 * @param count the skipping count
 * @return the new {@link FunctionalStream}
 * @see Stream#skip(long) for more information regarding this method
 */
fun <T> FunctionalStream<T>.skip(count: Long): FunctionalStream<T> {
    if (count <= 0) {
        throw IllegalArgumentException("count must be greater than 0")
    }
    val current = AtomicLong(0L)
    return filter { current.incrementAndGet() > count }
}

fun <T> FunctionalStream<T>.skipWhile(predicate: (T) -> Boolean): FunctionalStream<T> {
    val skip = AtomicBoolean(true)
    return filter {
        if (skip.get()) {
            skip.set(predicate(it))
        }
        skip.get()
    }
}

/**
 * Skip elements of this {@link FunctionalStream} until the
 * {@link Predicate} returns true.
 *
 * @param count the skipping count
 * @return the new {@link FunctionalStream}
 */
fun <T> FunctionalStream<T>.drop(count: Long): FunctionalStream<T> = skip(count)

fun <T> FunctionalStream<T>.dropWhile(predicate: (T) -> Boolean): FunctionalStream<T> = skipWhile(predicate)

/**
 * Keep a portion of this {@link FunctionalStream} by skipping
 * n elements and retaining n elements afterwards. This can be
 * used to implement paging.
 *
 * @param from which element should be the first in the {@link FunctionalStream}
 * @param to   which element should be the last in the {@link FunctionalStream}
 * @return the new {@link FunctionalStream}
 */
fun <T> FunctionalStream<T>.keep(from: Long, to: Long): FunctionalStream<T> {
    if (to < from) {
        throw IllegalArgumentException("to must be greater than from")
    }
    return skip(from).limit(to - from)
}

fun <T> FunctionalStream<T>.keepWhile(predicate: (T) -> Boolean): FunctionalStream<T> {
    return dropWhile(predicate).takeWhile { !predicate(it) }
}