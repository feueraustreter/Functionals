package feueraustreter.streamkt

import java.util.*
import java.util.stream.Stream

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
infix fun <T> FunctionalStream<T>.concat(other: FunctionalStream<T>): FunctionalStream<T> {
    return of(this, other).flatten()
}

/**
 * Calls {@link #concat(FunctionalStream)} after {@link #of(BaseStream)} with the given {@link Stream}.
 *
 * @param other the other {@link Stream}
 * @return the new {@link FunctionalStream}
 */
infix fun <T> FunctionalStream<T>.concat(other: Stream<T>): FunctionalStream<T> = concat(of(other))

infix fun <T> FunctionalStream<T>.concat(other: Iterable<T>): FunctionalStream<T> = concat(of(other))

infix fun <T> FunctionalStream<T>.concat(other: Iterator<T>): FunctionalStream<T> = concat(of(other))

fun <T> FunctionalStream<T>.insert(sink: (((T) -> Unit) -> Unit)): FunctionalStream<T> {
    val list = LinkedList<T>()
    sink { list.add(it) }
    return concat(ofWithoutComodification(list))
}

fun <T> FunctionalStream<T>.insert(sink: (((T) -> Unit) -> Unit), filter: ((T) -> Boolean)): FunctionalStream<T> {
    val list = LinkedList<T>()
    sink { if (filter(it)) list.add(it) }
    return concat(ofWithoutComodification(list))
}

fun <T> FunctionalStream<T>.duplicate(duplications: Long = 1): FunctionalStream<T> {
    return flatMap { generate({ l -> l <= duplications}) { it } }
}

fun <T> FunctionalStream<T>.duplicate(duplications: ((T) -> Long)): FunctionalStream<T> {
    return flatMap { generate({ l -> l <= duplications(it)}) { it } }
}

fun <T> FunctionalStream<T>.zipWithIndex(startIndex: Long = 0): FunctionalStream<Pair<T, Long>> {
    return indexMap { it, index -> Pair(it, index) }
}