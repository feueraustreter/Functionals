package feueraustreter.streamkt

import lombok.AllArgsConstructor
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.stream.Stream
import java.util.stream.StreamSupport

class FunctionalStreamImpl<T>: Iterable<T>, FunctionalStream<T> {

    private var virtualIndex = 0
    private var shortCircuit = AtomicBoolean(false)
    private var streamSource: Iterator<*>
    private var operations: MutableList<Any> = ArrayList()
    private var onClose: MutableSet<(() -> Unit)> = HashSet()
    private var onFinish: MutableSet<(() -> Unit)> = HashSet()
    private var onFinalize: MutableSet<(() -> Unit)> = HashSet()

    constructor(stream: FunctionalStreamImpl<*>) {
        virtualIndex = stream.virtualIndex + 1
        shortCircuit = stream.shortCircuit
        streamSource = stream.streamSource
        operations = stream.operations
        onClose = stream.onClose
        onFinish = stream.onFinish
        onFinalize = stream.onFinalize
    }

    constructor(streamSource: Iterator<*>) {
        this.streamSource = streamSource
    }

    override fun <K> map(mapper: Mapper<T, K>): FunctionalStream<K> {
        val shouldCreateNew = operations.isEmpty() || operations[operations.size - 1] !is Mapper<*, *>
        return if (shouldCreateNew) {
            val result: FunctionalStreamImpl<K> = FunctionalStreamImpl(this)
            result.operations.add(mapper)
            result
        } else {
            virtualIndex++
            operations.add(mapper)
            this as FunctionalStreamImpl<K>
        }
    }

    override fun filter(filter: Filter<T>): FunctionalStream<T> {
        val shouldCreateNew = operations.isEmpty() || operations[operations.size - 1] !is Filter<*>
        return if (shouldCreateNew) {
            val result: FunctionalStreamImpl<T> = FunctionalStreamImpl(this)
            result.operations.add(filter)
            result
        } else {
            virtualIndex++
            operations.add(filter)
            this
        }
    }

    override fun finalizeEach(runnable: Noop): FunctionalStream<T> {
        onFinalize.add(runnable)
        return this
    }

    override fun detach(): FunctionalStream<T> {
        val result: FunctionalStreamImpl<T> = FunctionalStreamImpl(this)
        result.virtualIndex = virtualIndex
        result.operations = java.util.ArrayList(operations)
        return result
    }

    override fun iterator(): Iterator<T> {
        val current = AtomicReference<T>()
        val hasNext = AtomicBoolean(false)
        if (hasNext()) {
            try {
                current.set(nextElement())
                hasNext.set(true)
            } catch (e: NoResultException) {
                hasNext.set(false)
            }
        }
        return object : Iterator<T> {
            override fun hasNext(): Boolean {
                return hasNext.get()
            }

            override fun next(): T {
                val currentValue = current.get()
                if (this@FunctionalStreamImpl.hasNext()) {
                    try {
                        current.set(this@FunctionalStreamImpl.nextElement())
                        hasNext.set(true)
                    } catch (e: NoResultException) {
                        hasNext.set(false)
                    }
                } else {
                    hasNext.set(false)
                }
                return currentValue
            }
        }
    }

    override fun spliterator(): Spliterator<T> {
        return object : Spliterator<T> {
            override fun tryAdvance(action: Consumer<in T>): Boolean {
                return if (hasNext()) {
                    try {
                        action.accept(nextElement())
                        onFinalize.forEach { it() }
                        true
                    } catch (e: NoResultException) {
                        false
                    }
                } else {
                    false
                }
            }

            override fun trySplit(): Spliterator<T>? {
                return null
            }

            override fun estimateSize(): Long {
                return Long.MAX_VALUE
            }

            override fun characteristics(): Int {
                return Spliterator.ORDERED
            }
        }
    }

    override fun toStream(): Stream<T> {
        return StreamSupport.stream(spliterator(), false)
    }

    override fun onClose(runnable: Noop): FunctionalStream<T> {
        onClose.add(runnable)
        return this
    }

    override fun onFinish(runnable: Noop): FunctionalStream<T> {
        onFinish.add(runnable)
        return this
    }

    override fun forEach(consumer: Consumer<in T>) {
        spliterator().forEachRemaining(consumer)
        onFinish.forEach { it() }
    }

    override fun close() {
        shortCircuit.set(true)
        onClose.forEach { it() }
    }

    override fun isClosed(): Boolean {
        return shortCircuit.get()
    }

    override fun hasNext(): Boolean {
        return streamSource.hasNext()
    }

    override fun nextElement(): T {
        while (true) {
            if (!hasNext() || isClosed()) {
                throw NoResultException()
            }
            return try {
                val result = streamSource.next()?.let { createResult(it, operations.size) }
                if (result == null) {
                    onFinalize.forEach { it() }
                    continue
                }
                result.value as T
            } catch (e: NoSuchElementException) {
                throw NoResultException(e.message, e)
            }
        }
    }

    private fun createResult(current: Any, to: Int): Result? {
        var current: Any? = current
        if (0 == to) {
            return Result(current)
        }
        for (i in 0 until to) {
            val operation: Any = operations[i]
            if (operation is Mapper<*, *>) {
                current = current?.let { (operation as Mapper<Any, Any>)(it) }
            } else if (operation is Filter<*>) {
                if (current == null || !(operation as Filter<Any>)(current)) {
                    return null
                }
            }
        }
        return Result(current)
    }
}

private operator fun <T> Predicate<T>.invoke(current: T): Boolean {
    return test(current)
}

@AllArgsConstructor
private class Result(val value: Any?)

private class NoResultException : RuntimeException {
    constructor() : super() {}
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
}