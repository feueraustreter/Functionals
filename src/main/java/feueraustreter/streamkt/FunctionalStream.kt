package feueraustreter.streamkt

import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Stream

interface FunctionalStream<T>: AutoCloseable {

    // Conversion methods

    /**
     * Convert this [FunctionalStream] of one type to another
     * by using the mapper [Function] provided. It will be applied
     * to every element of the current [FunctionalStream].
     *
     * @param <K>    the new type of the [FunctionalStream]
     * @param mapper the mapper [Function] to use
     * @return the new [FunctionalStream]
     * @see Stream.map
     */
    fun <K> map(mapper: Mapper<T, K>): FunctionalStream<K>

    /**
     * Retain anything in this [FunctionalStream] that matched
     * the given [Predicate]. Drops every element not matching it.
     *
     * @param filter the [Predicate] to use
     * @return the new [FunctionalStream]
     * @see Stream.filter
     */
    fun filter(filter: Filter<T>): FunctionalStream<T>

    fun finalizeEach(runnable: Noop): FunctionalStream<T> {
        throw UnsupportedOperationException("This method is not supported by this implementation")
    }

    fun detach(): FunctionalStream<T> {
        throw UnsupportedOperationException("This method is not supported by this implementation")
    }

    /**
     * @return a [Stream] of this [FunctionalStream].
     * @implNote This operation is optional.
     */
    fun toStream(): Stream<T> {
        throw UnsupportedOperationException()
    }

    // TODO: JavaDoc
    fun onClose(runnable: Noop): FunctionalStream<T> {
        throw UnsupportedOperationException()
    }

    // TODO: JavaDoc
    fun onFinish(runnable: Noop): FunctionalStream<T> {
        throw UnsupportedOperationException()
    }

    // Terminating methods

    /**
     * Terminate this [FunctionalStream] and apply the
     * given [Consumer] to every element left in the
     * [FunctionalStream].
     *
     * @param consumer the [Consumer] to use
     * @see Stream.forEach
     */
    fun forEach(consumer: Consumer<in T>)

    /**
     * Terminate this [FunctionalStream] without
     * evaluating the rest.
     *
     * @implSpec When closing a [FunctionalStream] in a terminating
     * operation the [FunctionalStream] should evaluate no further elements
     * and return what it has right now.
     * @implNote When you terminate a terminated [FunctionalStream] an
     * [Exception] should be thrown. Any subsequent calls to `#close()`
     * should be ignored. This terminating behaviour is crucial to some default
     * implementations.
     * @see Stream.close
     */
    override fun close()

    // TODO: JavaDoc
    fun isClosed(): Boolean

    // API for internal use cases, like concat()

    /**
     * This is used as a common API for the implementation. Calling from outside should
     * not be done.
     *
     * @return if this stream has at least one element left
     */
    operator fun hasNext(): Boolean {
        throw UnsupportedOperationException()
    }

    /**
     * This is used as a common API for the implementation. Calling from outside should
     * not be done.
     *
     * @return the next element of the underlying stream
     */
    fun nextElement(): T {
        throw UnsupportedOperationException()
    }
}

typealias FStream<T> = FunctionalStream<T>
typealias Noop = () -> Unit
typealias Sink<T> = (T) -> Unit
typealias Filter<T> = Predicate<T>
typealias Mapper<T, R> = (T) -> R
