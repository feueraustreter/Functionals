package feueraustreter.streamkt

operator fun <T> FunctionalStream<T>.plus(other: FunctionalStream<T>): FunctionalStream<T> {
    return this concat other
}

operator fun <T> FunctionalStream<T>.plus(other: T): FunctionalStream<T> {
    return this concat of(other)
}

operator fun <T> T.plus(other: FunctionalStream<T>): FunctionalStream<T> {
    return of(this) concat other
}

operator fun <T> FunctionalStream<T>.plus(other: Iterable<T>): FunctionalStream<T> {
    return this concat of(other)
}

operator fun <T> Iterable<T>.plus(other: FunctionalStream<T>): FunctionalStream<T> {
    return of(this) concat other
}

operator fun <T> FunctionalStream<T>.minus(other: T): FunctionalStream<T> {
    return filter { it != other }
}

operator fun <T> FunctionalStream<T>.minus(other: Iterable<T>): FunctionalStream<T> {
    return filter { !other.contains(it) }
}

operator fun <T, V> FunctionalStream<T>.rangeTo(other: FunctionalStream<V>): FunctionalStream<feueraustreter.utils.Pair<T, V>> {
    return this zip other
}

operator fun <T> FunctionalStream<T>.invoke() {
    forEach {}
}

operator fun <T> FunctionalStream<T>.invoke(sink: Sink<T>) {
    forEach(sink)
}

operator fun <T> FunctionalStream<T>.get(vararg indices: Long): FunctionalStream<T> {
    return indexFilter { it in indices }
}

operator fun <T> FunctionalStream<T>.get(vararg indices: Int): FunctionalStream<T> {
    return indexFilter { it.toInt() in indices }
}

operator fun <T> FunctionalStream<T>.get(range: ULongRange): FunctionalStream<T> {
    return indexFilter { it.toULong() in range }
}

operator fun <T> FunctionalStream<T>.get(range: LongRange): FunctionalStream<T> {
    return indexFilter { it in range }
}

operator fun <T> FunctionalStream<T>.get(range: IntRange): FunctionalStream<T> {
    return indexFilter { it in range }
}