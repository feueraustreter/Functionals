package feueraustreter.streamkt

import lombok.SneakyThrows
import java.io.InputStream
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.function.*
import java.util.function.Function
import java.util.stream.BaseStream

/**
 * Create a {@link FunctionalStream} of an existing {@link Iterable}.
 *
 * @param <K>      the {@link FunctionalStream} type to use
 * @param iterable the {@link Iterable} ElementSource
 * @return the new {@link FunctionalStream}
 */
fun <T> of(iterable: Iterable<T>): FunctionalStream<T> {
    return FunctionalStreamImpl(iterable.iterator())
}

/**
 * Create a [FunctionalStream] of an existing [Iterator].
 *
 * @param <K>      the [FunctionalStream] type to use
 * @param iterator the [Iterator] ElementSource
 * @return the new [FunctionalStream]
 */
fun <K> of(iterator: Iterator<K>): FunctionalStream<K> {
    return FunctionalStreamImpl(iterator)
}

fun <K> ofWithoutComodification(list: List<K>): FunctionalStream<K> {
    return of(object : Iterator<K> {
        private var index = 0
        override fun hasNext(): Boolean {
            return list.size > index
        }

        override fun next(): K {
            val current = index
            index++
            return list[current]
        }
    })
}

/**
 * Create a [FunctionalStream] of an existing [BaseStream].
 *
 * @param <K>    the [FunctionalStream] type to use
 * @param stream the [BaseStream] ElementSource
 * @return the new [FunctionalStream]
</K> */
fun <T, K : BaseStream<T, K>?> of(stream: BaseStream<T, K>): FunctionalStream<T> {
    return FunctionalStreamImpl(stream.iterator())
}

/**
 * Create a [FunctionalStream] of an existing [Map.entrySet].
 *
 * @param <K> the [FunctionalStream] type to use
 * @param map the [Map.entrySet] ElementSource
 * @return the new [FunctionalStream]
</K> */
fun <K, V> of(map: Map<K, V>): FunctionalStream<Map.Entry<K, V>> {
    return FunctionalStreamImpl(map.entries.iterator())
}

/**
 * See [InputStream.read].
 *
 * @param inputStream the InputStream to use
 * @return the new [FunctionalStream]
 */
fun of(inputStream: InputStream): FunctionalStream<Int> {
    return FunctionalStreamImpl(object : Iterator<Int> {
        @SneakyThrows
        override fun hasNext(): Boolean {
            return inputStream.available() > 0
        }

        @SneakyThrows
        override fun next(): Int {
            return inputStream.read()
        }
    })
}

/**
 * Returns an empty sequential [FunctionalStream].
 *
 * @param <K> the type of stream elements
 * @return an empty sequential [FunctionalStream]
</K> */
fun <K> empty(): FunctionalStream<K> {
    return FunctionalStreamImpl(object : Iterator<K> {
        override fun hasNext(): Boolean {
            return false
        }

        override fun next(): K {
            throw NoSuchElementException()
        }
    })
}

/**
 * Returns a sequential [FunctionalStream] containing a single element.
 *
 * @param element the single element
 * @param <K>     the type of stream elements
 * @return a singleton sequential [FunctionalStream]
</K> */
fun <K> of(element: K): FunctionalStream<K> {
    val atomicBoolean = AtomicBoolean(true)
    return FunctionalStreamImpl(object : Iterator<K> {
        override fun hasNext(): Boolean {
            return atomicBoolean.get()
        }

        override fun next(): K {
            atomicBoolean.set(false)
            return element
        }
    })
}

fun <K> ofSingle(supplier: Supplier<K>): FunctionalStream<K> {
    val atomicBoolean = AtomicBoolean(true)
    return FunctionalStreamImpl(object : Iterator<K> {
        override fun hasNext(): Boolean {
            return atomicBoolean.get()
        }

        override fun next(): K {
            atomicBoolean.set(false)
            return supplier.get()
        }
    })
}

/**
 * Returns a sequential ordered [FunctionalStream] whose elements are the specified values.
 *
 * @param <K>      the type of stream elements
 * @param elements the elements of the new stream
 * @return the new [FunctionalStream]
</K> */
@SafeVarargs
fun <K> of(vararg elements: K): FunctionalStream<K> {
    return FunctionalStreamImpl(elements.iterator())
}

/**
 * Returns an infinite sequential ordered [FunctionalStream] produced by iterative
 * application of a function `f` to an initial element `seed`,
 * producing a [FunctionalStream] consisting of `seed`, `f(seed)`,
 * `f(f(seed))`, etc.
 *
 *
 * The first element (position `0`) in the [FunctionalStream] will be
 * the provided `seed`.  For `n > 0`, the element at position
 * `n`, will be the result of applying the function `f` to the
 * element at position `n - 1`.
 *
 * @param <K>  the type of stream elements
 * @param seed the initial element
 * @param f    a function to be applied to the previous element to produce a new element
 * @return a new sequential [FunctionalStream]
</K> */
fun <K> iterate(seed: K, f: UnaryOperator<K>): FunctionalStream<K> {
    val current = AtomicReference(seed)
    return FunctionalStreamImpl(object : Iterator<K> {
        override fun hasNext(): Boolean {
            return true
        }

        override fun next(): K {
            val now = current.get()
            current.set(f.apply(now))
            return now
        }
    })
}

/**
 * Returns a sequential ordered [FunctionalStream] produced by iterative
 * application of the given `next` function to an initial element,
 * conditioned on satisfying the given `hasNext` predicate.  The
 * stream terminates as soon as the `hasNext` predicate returns false.
 *
 *
 * `iterate(Object, Predicate, UnaryOperator)`
 * should produce the same sequence of elements as produced by the corresponding for-loop:
 * <pre>`for (T index=seed; hasNext.test(index); index = next.apply(index)) {
 * ...
 * }
`</pre> *
 *
 *
 * The resulting sequence may be empty if the `hasNext` predicate
 * does not hold on the seed value.  Otherwise the first element will be the
 * supplied `seed` value, the next element (if present) will be the
 * result of applying the `next` function to the `seed` value,
 * and so on iteratively until the `hasNext` predicate indicates that
 * the stream should terminate.
 *
 * @param <K>     the type of stream elements
 * @param seed    the initial element
 * @param hasNext a predicate to apply to elements to determine when the stream must terminate.
 * @param next    a function to be applied to the previous element to produce a new element
 * @return a new sequential [FunctionalStream]
</K> */
fun <K> iterate(seed: K, hasNext: Predicate<in K>, next: UnaryOperator<K>): FunctionalStream<K> {
    val current = AtomicReference(seed)
    return FunctionalStreamImpl(object : Iterator<K> {
        override fun hasNext(): Boolean {
            return hasNext.test(current.get())
        }

        override fun next(): K {
            val now = current.get()
            current.set(next.apply(now))
            return now
        }
    })
}

// TODO: JavaDoc
fun iterateInt(initial: Int, end: Int, increment: Int): FunctionalStream<Int> {
    if (increment == 0) {
        throw UnsupportedOperationException()
    }
    if (end > initial && increment < 0) {
        throw UnsupportedOperationException()
    }
    return iterate(initial, { l: Int -> l < end }) { l: Int -> l + increment }
}

// TODO: JavaDoc
fun iterateInt(initial: Int, end: Int): FunctionalStream<Int> {
    return iterateInt(initial, end, 1)
}

// TODO: JavaDoc
fun iterateLong(initial: Long, end: Long, increment: Long): FunctionalStream<Long> {
    if (increment == 0L) {
        throw UnsupportedOperationException()
    }
    if (end > initial && increment < 0) {
        throw UnsupportedOperationException()
    }
    return iterate(initial, { l: Long -> l < end }) { l: Long -> l + increment }
}

// TODO: JavaDoc
fun iterateLong(initial: Long, end: Long): FunctionalStream<Long> {
    return iterateLong(initial, end, 1)
}

// TODO: JavaDoc
fun iterateFloat(initial: Float, end: Float, increment: Float): FunctionalStream<Float> {
    return iterate(initial, { l: Float -> l < end }) { l: Float -> l + increment }
}

// TODO: JavaDoc
fun iterateDouble(initial: Double, end: Double, increment: Double): FunctionalStream<Double> {
    return iterate(initial, { l: Double -> l < end }) { l: Double -> l + increment }
}

fun <K> infinite(): FunctionalStream<K?> {
    return infinite(null)
}

fun <K> infinite(element: K): FunctionalStream<K> {
    return FunctionalStreamImpl(object : Iterator<K> {
        override fun hasNext(): Boolean {
            return true
        }

        override fun next(): K {
            return element
        }
    })
}

fun <K> random(random: Random?, generator: Function<Random?, K>): FunctionalStream<K> {
    return FunctionalStreamImpl(object : Iterator<K> {
        override fun hasNext(): Boolean {
            return true
        }

        override fun next(): K {
            return generator.apply(random)
        }
    })
}

fun <K> random(random: Random?, generator: Function<Random?, K>, count: Long): FunctionalStream<K> {
    val remaining = AtomicLong(count)
    return FunctionalStreamImpl(object : Iterator<K> {
        override fun hasNext(): Boolean {
            return remaining.get() > 0
        }

        override fun next(): K {
            if (remaining.getAndDecrement() <= 0) {
                throw NoSuchElementException()
            }
            return generator.apply(random)
        }
    })
}

/**
 * Returns an infinite sequential unordered stream where each element is
 * generated by the provided `Supplier`.  This is suitable for
 * generating constant streams, streams of random elements, etc.
 *
 * @param <K> the type of stream elements
 * @param s   the `Supplier` of generated elements
 * @return a new infinite sequential unordered [FunctionalStream]
</K> */
fun <K> generate(s: Supplier<out K>): FunctionalStream<K> {
    return FunctionalStreamImpl(object : Iterator<K> {
        override fun hasNext(): Boolean {
            return true
        }

        override fun next(): K {
            return s.get()
        }
    })
}

// TODO: JavaDoc
fun <K> generate(hasNext: LongPredicate, s: Supplier<out K>): FunctionalStream<K> {
    val index = AtomicLong(0)
    return FunctionalStreamImpl(object : Iterator<K> {
        override fun hasNext(): Boolean {
            return hasNext.test(index.get())
        }

        override fun next(): K {
            index.incrementAndGet()
            return s.get()
        }
    })
}

// TODO: JavaDoc
fun <K> generate(hasNext: BooleanSupplier, next: Supplier<out K>): FunctionalStream<K> {
    return FunctionalStreamImpl(object : Iterator<K> {
        override fun hasNext(): Boolean {
            return hasNext.asBoolean
        }

        override fun next(): K {
            return next.get()
        }
    })
}