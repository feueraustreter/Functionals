package feueraustreter.streamkt

import feueraustreter.utils.Pair
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function

fun <T: Comparable<T>> FunctionalStream<T>.sorted(comparator: Comparator<T> = Comparator { a, b -> a.compareTo(b)}): FunctionalStream<T> {
    val current: FunctionalStream<FunctionalStream<T>> = batchViaCollections(1000).map(Function { it.sortedViaCollections(comparator) })
    return of(object : Iterator<FunctionalStream<T>> {
        private var hasNext = true
        override fun hasNext(): Boolean {
            return hasNext
        }

        override fun next(): FunctionalStream<T> {
            hasNext = false
            return current.reduce { ts, ts2 -> ts.merge(ts2, comparator) }
        }
    }).flatten()
}

fun <T: Comparable<T>> FunctionalStream<T>.sortedViaCollections(comparator: Comparator<T> = Comparator { a, b -> a.compareTo(b)}): FunctionalStream<T> {
    val current: FunctionalStream<T> = this
    val elements = AtomicReference<MutableList<T>>(null)
    val elementsCreator = label@ {
        if (elements.get() != null) return@label
        elements.set(ArrayList())
        current.forEach { elements.get()!!.add(it) }
        elements.get()!!.sortWith(comparator)
    }
    return ofSingle<List<T>> {
        elementsCreator()
        elements.get()
    }.flatCollectionMap { it }
}

fun <T> FunctionalStream<T>.reverse(): FunctionalStream<T> {
    val current: FunctionalStream<T> = this
    val elements = AtomicReference<MutableList<T>>(null)
    val elementsCreator = label@ {
        if (elements.get() != null) return@label
        elements.set(ArrayList())
        current.forEach { elements.get()!!.add(it) }
        elements.get()!!.reverse()
    }
    return ofSingle<List<T>> {
        elementsCreator()
        elements.get()
    }.flatCollectionMap { it }
}

fun <T> FunctionalStream<T>.merge(other: FunctionalStream<T>, comparator: Comparator<T>): FunctionalStream<T> {
    val current: FunctionalStream<T> = this
    return of(object : Iterator<T> {
        private var elementThis: AtomicReference<T>? = null
        private var elementOther: AtomicReference<T>? = null

        override fun hasNext(): Boolean {
            return current.hasNext() || other.hasNext() || elementThis != null || elementOther != null
        }

        override fun next(): T {
            if (elementThis == null && current.hasNext()) {
                elementThis = AtomicReference(current.nextElement())
            }
            if (elementOther == null && other.hasNext()) {
                elementOther = AtomicReference(other.nextElement())
            }
            if (elementThis != null && elementOther == null) {
                val result = elementThis!!.get()
                elementThis = null
                return result
            }
            if (elementThis == null && elementOther != null) {
                val result = elementOther!!.get()
                elementOther = null
                return result
            }
            if (elementThis == null) {
                throw NoSuchElementException()
            }
            if (comparator.compare(elementThis!!.get(), elementOther!!.get()) <= 0) {
                val result = elementThis!!.get()
                elementThis = null
                return result
            }
            val result = elementOther!!.get()
            elementOther = null
            return result
        }
    })
}

fun <T, K> FunctionalStream<T>.flatMerge(mapper: ((T) -> FunctionalStream<K>), comparator: Comparator<K>) {
    val current = map(mapper)
    return of(object : Iterator<K> {
        private var hasNext = true

        override fun hasNext(): Boolean {
            return hasNext
        }

        override fun next(): K {
            hasNext = false
            return current.reduce { ts, ts2 -> ts.merge(ts2, comparator) }
        }
    }).flatten()
}

fun <T, V> FunctionalStream<T>.merge(other: FunctionalStream<V>): FunctionalStream<Pair<T, V>> {
    return zip(other) { k, v -> feueraustreter.utils.Pair(k, v) }
}

fun <T, O, K> FunctionalStream<T>.merge(other: FunctionalStream<O>, zipper: ((T, O?) -> K)): FunctionalStream<K> {
    return zip(other, zipper)
}

fun <T, V> FunctionalStream<T>.zip(other: FunctionalStream<V>): FunctionalStream<Pair<T, V>> {
    return zip(other) { k, v -> feueraustreter.utils.Pair(k, v) }
}

fun <T, O, K> FunctionalStream<T>.zip(other: FunctionalStream<O>, zipper: (T, O?) -> K): FunctionalStream<K> {
    return map {
        try {
            zipper(it, other.nextElement())
        } catch (e: NoSuchElementException) {
            zipper(it, null)
        }
    }
}

fun <T> FunctionalStream<T>.makeBucketsWithCounts(): FunctionalStream<Pair<T, Long>> {
    return makeBuckets({ 1L }) { it + 1 }
}

fun <T, V> FunctionalStream<T>.makeBuckets(initialValue: (() -> V), valueMutator: ((V) -> V)): FunctionalStream<Pair<T, V>> {
    val current = this
    val elements = AtomicReference<Map<T, V>>(null)
    val elementsList = AtomicReference<MutableList<Map.Entry<T, V>>>(null)
    val elementsCreator = label@ {
        if (elements.get() != null) return@label
        elements.set(HashMap())
        current.forEach { t: T ->
            val map: MutableMap<T, V> = elements.get()
            var value = map[t]
            if (value == null) {
                value = initialValue()
            } else {
                value = valueMutator(value)
            }
            map[t] = value
        }
        elementsList.set(ArrayList(elements.get().entries))
    }
    return of(object : Iterator<Pair<T, V>> {
        override fun hasNext(): Boolean {
            elementsCreator()
            return elementsList.get().isNotEmpty()
        }

        override fun next(): Pair<T, V> {
            elementsCreator()
            return Pair.of(elementsList.get().removeAt(0))
        }
    })
}

fun <T, K> FunctionalStream<T>.mapWithSizeOfStream(mapper: ((T, Long) -> K)): FunctionalStream<K> {
    val current = this
    val elements = AtomicReference<MutableList<K>>(null)
    val elementsCreator = label@ {
        if (elements.get() != null) return@label
        elements.set(ArrayList())
        val list: List<T> = current.toList()
        list.forEach { elements.get().add(mapper(it, list.size.toLong())) }
    }
    return of(object : Iterator<K> {
        override fun hasNext(): Boolean {
            elementsCreator()
            return elements.get().isNotEmpty()
        }

        override fun next(): K {
            elementsCreator()
            return elements.get().removeAt(0)
        }
    })
}