package feueraustreter.streamkt

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collector
import java.util.stream.Collectors

fun <T> FunctionalStream<T>.higherOrderForEach(higherOrderForEach: ((T) -> ((T) -> Unit))) {
    forEach {
        higherOrderForEach(it)(it)
    }
}

fun <T> FunctionalStream<T>.timeIt(): Long {
    val start = System.currentTimeMillis()
    forEach { }
    return System.currentTimeMillis() - start
}

/**
 * Terminate this {@link FunctionalStream} and collect
 * every element left in this {@link FunctionalStream}
 * into a {@link List}.
 *
 * @return the {@link List} of elements
 */
fun <T> FunctionalStream<T>.toList(): MutableList<T> {
    return collect(Collectors.toList())
}

fun <T> FunctionalStream<T>.toList(list: MutableList<T>): MutableList<T> {
    forEach { list.add(it) }
    return list
}

fun <T> FunctionalStream<T>.toSet(): MutableSet<T> {
    return collect(Collectors.toSet())
}

fun <T> FunctionalStream<T>.toSet(set: MutableSet<T>): MutableSet<T> {
    forEach { set.add(it) }
    return set
}

fun <T> FunctionalStream<T>.toMap(): MutableMap<T, T> {
    return collect(Collectors.toMap({ it }, { it }))
}

fun <T, V> FunctionalStream<T>.toMap(valueFunction: ((T) -> V)): MutableMap<T, V> {
    return collect(Collectors.toMap({ it }, valueFunction))
}

fun <T> FunctionalStream<T>.toMap(map: MutableMap<T, T>): MutableMap<T, T> {
    forEach { map[it] = it }
    return map
}

fun <T> FunctionalStream<T>.toMap(map: MutableMap<T, T>, valueFunction: ((T) -> T)): MutableMap<T, T> {
    forEach { map[it] = valueFunction(it) }
    return map
}

/**
 * Terminate this {@link FunctionalStream} and collect
 * every element left in this {@link FunctionalStream}
 * into a {@link String} with a specific delimiter.
 *
 * @param separator the delimiter to use
 * @param prefix the prefix to use
 * @param suffix the suffix to use
 * @return the {@link String} of every element joined by the delimiter
 */
fun FunctionalStream<String>.joining(separator: String = "", prefix: String = "", suffix: String = ""): String {
    return collect({ StringJoiner(separator, prefix, suffix) }, { container, element -> container.add(element) }, { it.toString() })
}

/**
 * Terminate this {@link FunctionalStream} and collect it
 * to a specific type by using a {@link Collector}. Some
 * common {@link Collector}'s can be found in the
 * {@link Collectors} class.
 *
 * @param <R>       the type of the result
 * @param <A>       the intermediate accumulation type of the {@link Collector}
 * @param collector the {@link Collector} describing the reduction
 * @return the result of the reduction
 * @see Collectors
 * @see Stream#collect(Collector) for more information regarding this method
 */
fun <T, R, A> FunctionalStream<T>.collect(collector: Collector<T, A, R>): R {
    val container = collector.supplier().get()
    val biConsumer = collector.accumulator()
    forEach { biConsumer.accept(container, it) }
    return collector.finisher().apply(container)
}

fun <T, R, A> FunctionalStream<T>.collect(containerCreator: () -> A, accumulator: (A, T) -> A, finisher: (A) -> R): R {
    var container = containerCreator()
    forEach { container = accumulator(container, it) }
    return finisher(container)
}

/**
 * Terminate and evaluate every statement of this {@link FunctionalStream}
 * without any return value.
 */
fun <T> FunctionalStream<T>.eval() {
    forEach { }
}

/**
 * Terminate and evaluate this {@link FunctionalStream} but stop on the first
 * result found. This is the equivalent of calling {@link #findFirst()} and
 * ignoring the return value.
 */
fun <T> FunctionalStream<T>.evalFirst() {
    findFirst()
}

/**
 * Terminate this {@link FunctionalStream} and
 * check if one element can be found that qualifies to
 * the {@link Predicate} given. The {@link Predicate}
 * does not need to be evaluated on every element to
 * determine the output of this call.
 *
 * @param predicate the {@link Predicate} to use
 * @return if any element matched the {@link Predicate}
 * @see Stream#anyMatch(Predicate) for more information regarding this method
 */
fun <T> FunctionalStream<T>.anyMatch(filter: Filter<T>): Boolean {
    return !noneMatch(filter)
}

/**
 * Terminate this {@link FunctionalStream} and
 * check if every element qualifies to the
 * {@link Predicate} given. The {@link Predicate}
 * does not need to be evaluated on every element
 * to determine the output of this call.
 *
 * @param predicate the {@link Predicate} to use
 * @return if every element matched the {@link Predicate}
 * @see Stream#allMatch(Predicate) for more information regarding this method
 */
fun <T> FunctionalStream<T>.allMatch(filter: Filter<T>): Boolean {
    return noneMatch(filter.negate())
}

/**
 * Terminate this {@link FunctionalStream} and
 * check if no element qualified to the
 * {@link Predicate} given. The {@link Predicate}
 * does not need to be evaluated on every element
 * to determine the output of this call.
 *
 * @param predicate the {@link Predicate} to use
 * @return if no element matched the {@link Predicate}
 * @see Stream#noneMatch(Predicate) for more information regarding this method
 */
fun <T> FunctionalStream<T>.noneMatch(filter: Filter<T>): Boolean {
    val found = AtomicBoolean(true)
    filter(filter).forEach {
        found.set(false)
        close()
    }
    return found.get()
}

/**
 * Terminate this {@link FunctionalStream} and
 * count the elements left in this
 * {@link FunctionalStream}.
 *
 * @return the element count of this {@link FunctionalStream}
 * @see Stream#count() for more information regarding this method
 */
fun <T> FunctionalStream<T>.count(): Long {
    val result = AtomicLong()
    forEach { result.incrementAndGet() }
    return result.get()
}

fun <T> FunctionalStream<T>.size(): Long {
    return count()
}

/**
 * Terminate this {@link FunctionalStream} and return
 * the first element in this {@link FunctionalStream}.
 * If this {@link FunctionalStream} is empty or the
 * Element is {@code null} {@link Optional#empty()}
 * gets returned.
 *
 * @return the first element of this {@link FunctionalStream} or none.
 * @see Stream#findFirst() for more information regarding this method
 */
fun <T> FunctionalStream<T>.findFirst(filter: Filter<T> = Filter { true }): Optional<T> {
    val result = AtomicReference(Optional.empty<T>())
    filter(filter).forEach {
        result.set(Optional.ofNullable(it))
        close()
    }
    return result.get()
}

/**
 * Terminate this {@link FunctionalStream} and return
 * the last element in this {@link FunctionalStream}.
 * If this {@link FunctionalStream} is empty or the
 * Element is {@code null} {@link Optional#empty()}
 * gets returned.
 *
 * @return the first element of this {@link FunctionalStream} or none.
 * @see Stream#findFirst() for more information regarding this method
 */
fun <T> FunctionalStream<T>.findLast(filter: Filter<T> = Filter { true }): Optional<T> {
    val result = AtomicReference(Optional.empty<T>())
    filter(filter).forEach {
        result.set(Optional.ofNullable(it))
    }
    return result.get()
}

/**
 * Terminate this {@link FunctionalStream} and return
 * the smallest element determined by the {@link Comparator}
 * given. If this {@link FunctionalStream} is empty
 * {@link Optional#empty()} gets returned.
 *
 * @param comparator the {@link Comparator} to compare the elements
 * @return the smallest element of this {@link FunctionalStream} or none.
 * @see Stream#min(Comparator) for more information regarding this method
 */
fun <T> FunctionalStream<T>.min(comparator: Comparator<T>): Optional<T> {
    val result = AtomicReference(Optional.empty<T>())
    forEach { t ->
        val current = result.get()
        if (!current.isPresent) {
            result.set(Optional.ofNullable(t))
            return@forEach
        }
        if (comparator.compare(current.get(), t) > 0) {
            result.set(Optional.ofNullable(t))
        }
    }
    return result.get()
}

/**
 * Terminate this {@link FunctionalStream} and return
 * the smallest element determined by natural ordering.
 * If this {@link FunctionalStream} is empty {@link Optional#empty()}
 * gets returned. A {@link ClassCastException} can be thrown
 * when the type 'T' is not a {@link Comparable}.
 *
 * @return the smallest element of this {@link FunctionalStream} or none.
 * @see Stream#min(Comparator) for more information regarding this method
 */
fun <T : Comparable<T>> FunctionalStream<T>.min(): Optional<T> {
    return min(Comparator.naturalOrder())
}

/**
 * Terminate this {@link FunctionalStream} and return
 * the biggest element determined by the {@link Comparator}
 * given. If this {@link FunctionalStream} is empty
 * {@link Optional#empty()} gets returned.
 *
 * @param comparator the {@link Comparator} to compare the elements
 * @return the biggest element of this {@link FunctionalStream} or none.
 * @see Stream#max(Comparator) for more information regarding this method
 */
fun <T> FunctionalStream<T>.max(comparator: Comparator<T>): Optional<T> {
    val result = AtomicReference(Optional.empty<T>())
    forEach { t ->
        val current = result.get()
        if (!current.isPresent) {
            result.set(Optional.ofNullable(t))
            return@forEach
        }
        if (comparator.compare(current.get(), t) < 0) {
            result.set(Optional.ofNullable(t))
        }
    }
    return result.get()
}

/**
 * Terminate this {@link FunctionalStream} and return
 * the biggest element determined by natural ordering.
 * If this {@link FunctionalStream} is empty {@link Optional#empty()}
 * gets returned. A {@link ClassCastException} can be thrown
 * when the type 'T' is not a {@link Comparable}.
 *
 * @return the smallest element of this {@link FunctionalStream} or none.
 * @see Stream#max(Comparator) for more information regarding this method
 */
fun <T : Comparable<T>> FunctionalStream<T>.max(): Optional<T> {
    return max(Comparator.naturalOrder())
}

/**
 * Terminate and reduce this {@link FunctionalStream} to
 * a {@link Float}. The {@link Function} will map every
 * element to type {@link Float}.
 *
 * @param floatFunction the {@link Function} to produce the {@link Float}'s
 * @param identity      the identity value for the reduction
 * @return the sum of every {@link Float}
 */
fun FunctionalStream<Float>.sum(identity: Float = 0f): Float {
    return reduce(identity) { a, b -> a + b }
}

/**
 * Terminate and reduce this {@link FunctionalStream} to
 * a {@link Float}.
 *
 * @param identity      the identity value for the reduction
 * @return the multiplication of every {@link Float}
 */
fun FunctionalStream<Float>.multiplication(identity: Float = 1f): Float {
    return reduce(identity) { a, b -> a * b }
}

/**
 * Terminate and reduce this {@link FunctionalStream} to
 * a {@link Float}.
 *
 * @param identity      the identity value for the reduction
 * @return the multiplication of every {@link Float}
 */
fun FunctionalStream<Float>.average(identity: Float = 0f): Float {
    val size = AtomicLong(0)
    return collect({ identity }, { a, b ->
        size.incrementAndGet()
        a + b
    }) { it / size.get() }
}

/**
 * Terminate and reduce this {@link FunctionalStream} to
 * a {@link Double}.
 *
 * @param identity       the identity value for the reduction
 * @return the sum of every {@link Double}
 */
fun FunctionalStream<Double>.sum(identity: Double = 0.0): Double {
    return reduce(identity) { a, b -> a + b }
}

/**
 * Terminate and reduce this {@link FunctionalStream} to
 * a {@link Double}.
 *
 * @param identity       the identity value for the reduction
 * @return the multiplication of every {@link Double}
 */
fun FunctionalStream<Double>.multiplication(identity: Double = 1.0): Double {
    return reduce(identity) { a, b -> a * b }
}

/**
 * Terminate and reduce this {@link FunctionalStream} to
 * a {@link Double}.
 *
 * @param identity      the identity value for the reduction
 * @return the multiplication of every {@link Double}
 */
fun FunctionalStream<Double>.average(identity: Double = 0.0): Double {
    val size = AtomicLong(0)
    return collect({ identity }, { a, b ->
        size.incrementAndGet()
        a + b
    }) { it / size.get() }
}

/**
 * Terminate and reduce this {@link FunctionalStream} to
 * a {@link Integer}.
 *
 * @param identity        the identity value for the reduction
 * @return the sum of every {@link Integer}
 */
fun FunctionalStream<Int>.sum(identity: Int = 0): Int {
    return reduce(identity) { a, b -> a + b }
}

/**
 * Terminate and reduce this {@link FunctionalStream} to
 * a {@link Integer}.
 *
 * @param identity        the identity value for the reduction
 * @return the multiplication of every {@link Integer}
 */
fun FunctionalStream<Int>.multiplication(identity: Int = 1): Int {
    return reduce(identity) { a, b -> a * b }
}

/**
 * Terminate and reduce this {@link FunctionalStream} to
 * a {@link Int}.
 *
 * @param identity      the identity value for the reduction
 * @return the multiplication of every {@link Int}
 */
fun FunctionalStream<Int>.average(identity: Int = 0): Int {
    val size = AtomicLong(0)
    return collect({ identity }, { a, b ->
        println("a: $a, b: $b   size: $size")
        size.incrementAndGet()
        a + b
    }) { (it / size.get()).toInt() }
}

/**
 * Terminate and reduce this {@link FunctionalStream} to
 * a {@link Long}.
 *
 * @param identity     the identity value for the reduction
 * @return the sum of every {@link Long}
 */
fun FunctionalStream<Long>.sum(identity: Long = 0): Long {
    return reduce(identity) { a, b -> a + b }
}

/**
 * Terminate and reduce this {@link FunctionalStream} to
 * a {@link Long}.
 *
 * @param identity     the identity value for the reduction
 * @return the multiplication of every {@link Long}
 */
fun FunctionalStream<Long>.multiplication(identity: Long = 1): Long {
    return reduce(identity) { a, b -> a * b }
}

/**
 * Terminate and reduce this {@link FunctionalStream} to
 * a {@link Long}.
 *
 * @param identity      the identity value for the reduction
 * @return the multiplication of every {@link Long}
 */
fun FunctionalStream<Long>.average(identity: Long = 0): Long {
    val size = AtomicLong(0)
    return collect({ identity }, { a, b ->
        size.incrementAndGet()
        a + b
    }) { it / size.get() }
}

fun <T> FunctionalStream<T>.reduce(accumulator: ((T, T) -> T)): T {
    val ref = AtomicReference<T>(null)
    val wasFirst = AtomicBoolean(true)
    forEach { t: T ->
        if (wasFirst.getAndSet(false)) {
            ref.set(t)
        } else {
            ref.set(accumulator(ref.get(), t))
        }
    }
    return ref.get()
}

/**
 * Terminate this {@link FunctionalStream} and return
 * a single return element determined by a given
 * 'identity' and an {@link BinaryOperator} to mutate
 * this initial 'identity' until every element is
 * used.
 *
 * @param identity    the initial value
 * @param accumulator the accumulator to mutate the value
 * @return the single return element
 * @see Stream#reduce(Object, BinaryOperator) for more information regarding this method
 */
fun <T> FunctionalStream<T>.reduce(identity: T, accumulator: ((T, T) -> T)): T {
    val result = AtomicReference(identity)
    forEach { t: T -> result.set(accumulator(result.get(), t)) }
    return result.get()
}

inline fun <reified T> FunctionalStream<T>.toArray(): Array<T> {
    val list: List<T> = toList()
    return list.toTypedArray()
}