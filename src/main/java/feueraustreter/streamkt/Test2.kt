package feueraustreter.streamkt

fun main() {
    of(1, 2, 3, 4, 5)
        .map { it * 2 }
        .filter { it < 5 }
        .forEach { println(it) }
}