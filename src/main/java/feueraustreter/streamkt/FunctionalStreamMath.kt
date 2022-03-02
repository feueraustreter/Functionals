package feueraustreter.streamkt

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

fun <T> FunctionalStream<T>.scan(mutator: ((T, T) -> T)): FunctionalStream<T> {
    val currentValue = AtomicReference<T>(null)
    val wasFirst = AtomicBoolean(true)
    return map {
        if (wasFirst.get()) {
            wasFirst.set(false)
            currentValue.set(it)
            return@map currentValue.get()
        }
        val newValue: T = mutator(currentValue.get(), it)
        currentValue.set(newValue)
        newValue
    }
}