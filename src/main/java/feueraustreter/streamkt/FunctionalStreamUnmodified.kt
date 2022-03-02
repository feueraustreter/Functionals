package feueraustreter.streamkt

/**
 * This can be used to debug the {@link FunctionalStreamOld} or
 * call one specific thing on every element of the {@link FunctionalStreamOld}.
 *
 * @param consumer the {@link Consumer} to call
 * @return the new {@link FunctionalStreamOld}
 * @see Stream#peek(Consumer) for more information regarding this method
 */
fun <T> FunctionalStream<T>.peek(consumer: ((T) -> Unit)): FunctionalStream<T> {
    return filter {
        consumer(it)
        true
    }
}

fun <T> FunctionalStream<T>.each(consumer: ((T) -> Unit)): FunctionalStream<T> = peek(consumer)

fun <T> FunctionalStream<T>.peek(consumer: ((T) -> Unit), condition: ((T) -> Boolean)): FunctionalStream<T> {
    return filter {
        if (condition(it)) {
            consumer(it)
        }
        true
    }
}

fun <T> FunctionalStream<T>.each(consumer: ((T) -> Unit), condition: ((T) -> Boolean)): FunctionalStream<T> = peek(consumer, condition)

fun <T> FunctionalStream<T>.higherOrderPeek(consumer: ((T) -> ((T) -> Unit))): FunctionalStream<T> {
    return filter {
        consumer(it)(it)
        true
    }
}

fun <T> FunctionalStream<T>.higherOrderEach(consumer: ((T) -> ((T) -> Unit))): FunctionalStream<T> = higherOrderPeek(consumer)

/**
 * This can be used to debug the {@link FunctionalStreamOld} and get
 * every element in a {@link Collection}.
 *
 * @param collection the {@link Collection} to put every element into
 * @return the new {@link FunctionalStreamOld}
 */
fun <T> FunctionalStream<T>.peek(collection: MutableCollection<T>): FunctionalStream<T> {
    return filter {
        collection.add(it)
        true
    }
}

fun <T> FunctionalStream<T>.each(collection: MutableCollection<T>): FunctionalStream<T> = peek(collection)

fun <T> FunctionalStream<T>.inline(runnable: (() -> Unit)): FunctionalStream<T> {
    return filter {
        runnable()
        true
    }
}

fun <T> FunctionalStream<T>.each(runnable: (() -> Unit)): FunctionalStream<T> = inline(runnable)

fun <T> FunctionalStream<T>.inline(runnable: (() -> Unit), condition: ((T) -> Boolean)): FunctionalStream<T> {
    return filter {
        if (condition(it)) {
            runnable()
        }
        true
    }
}

fun <T> FunctionalStream<T>.each(runnable: (() -> Unit), condition: ((T) -> Boolean)): FunctionalStream<T> = inline(runnable, condition)