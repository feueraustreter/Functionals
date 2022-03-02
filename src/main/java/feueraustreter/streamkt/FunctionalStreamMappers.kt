package feueraustreter.streamkt

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.BaseStream
import java.util.stream.Stream

// TODO: JavaDoc
fun <T, K> FunctionalStream<T>.indexMap(mapper: ((T, Long) -> K)): FunctionalStream<K> {
    val atomicLong = AtomicLong()
    return map { mapper(it, atomicLong.getAndIncrement()) }
}

// TODO: JavaDoc
fun <T, K> FunctionalStream<T>.higherOrderMap(higherOrderMap: ((T) -> ((T) -> K))): FunctionalStream<K> {
    return map { higherOrderMap(it)(it) }
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
fun <T, K> FunctionalStream<T>.flatMap(mapper: ((T) -> FunctionalStream<K>)): FunctionalStream<K> {
    val current: FunctionalStream<T> = this
    return of(object : Iterator<K> {
        private var currentStream: AtomicReference<FunctionalStream<K>>? = null
        override fun hasNext(): Boolean {
            return current.hasNext() || currentStream!!.get().hasNext()
        }

        override fun next(): K {
            if (currentStream == null) {
                currentStream = AtomicReference(mapper(current.nextElement()))
            }
            currentStream = try {
                return currentStream!!.get().nextElement()
            } catch (e: Exception) {
                null
            }
            if (currentStream == null) {
                currentStream = AtomicReference(mapper(current.nextElement()))
            }
            return try {
                currentStream!!.get().nextElement()
            } catch (e: Exception) {
                throw NoSuchElementException()
            }
        }
    })
}

// TODO: JavaDoc
fun <T> FunctionalStream<FunctionalStream<T>>.flatten(): FunctionalStream<T> {
    return flatMap { it }
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
fun <T, K> FunctionalStream<T>.flatArrayMap(mapper: ((T) -> Array<K>)): FunctionalStream<K> {
    return flatMap { of(*mapper(it)) }
}

// TODO: JavaDoc
fun <T, K, L: BaseStream<K, L>> FunctionalStream<T>.flatStreamMap(mapper: ((T) -> BaseStream<K, L>)): FunctionalStream<K> {
    return flatMap { of(mapper(it)) }
}

// TODO: JavaDoc
fun <T, K> FunctionalStream<T>.flatIteratorMap(mapper: ((T) -> Iterator<K>)): FunctionalStream<K> {
    return flatMap { of(mapper(it)) }
}

// TODO: JavaDoc
fun <T, K> FunctionalStream<T>.flatIterableMap(mapper: ((T) -> Iterable<K>)): FunctionalStream<K> {
    return flatMap { of(mapper(it)) }
}

// TODO: JavaDoc
fun <T, K> FunctionalStream<T>.flatCollectionMap(mapper: ((T) -> Collection<K>)): FunctionalStream<K> {
    return flatMap { of(mapper(it)) }
}

// TODO: JavaDoc
fun <T, K, V> FunctionalStream<T>.flatMapMap(mapper: ((T) -> Map.Entry<K, V>)): FunctionalStream<Map.Entry<K, V>> {
    return flatMap { of(mapper(it)) }
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
fun <T> FunctionalStream<T>.partialMap(filter: (T) -> Boolean, mapper: ((T) -> T)): FunctionalStream<T> {
    return map {
        if (filter(it)) {
            mapper(it)
        } else {
            it
        }
    }
}

fun <T, K> FunctionalStream<T>.forkingMap(filter: (T) -> Boolean, filteredMapper: ((T) -> K), unfilteredMapper: ((T) -> K)): FunctionalStream<K> {
    return map {
        if (filter(it)) {
            filteredMapper(it)
        } else {
            unfilteredMapper(it)
        }
    }
}