package feueraustreter.streamkt

fun main() {
    of(1, 2, 3, 4, 5)
        .map { it * 2 }
        .filter { it < 5 }
        .forEach { println(it) }

    of(1, 2, 3, 4, 5)() {
        println(it * 2)
    }

    (0 + empty() + 1 + 2 + 3 + 4 + 5) {
        println(it)
    }
    (empty<Int>() + 0 + 1 + 2 + 3 + 4)
        .map { it * 2 }
        .filter { it < 5 }
        .forEach { println(it) }

    (of(1, 2, 3, 4, 5) - 4) {
        println(it)
    }

    println()

    for (i in of(1, 2, 4, 5).iterable()) {

    }
}