/*
 * Copyright 2021 feueraustreter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package feueraustreter.stream;

import feueraustreter.utils.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Test {

    public static void main(String[] args) {
        // test1();
        // test2();
        // test5();
        // test6();
        // test7();
        // test8();
        // test9();
        // test10();
        // test11();
        // test12();
        // test13();
        // test14();
        // test15();
        // test16();
        // test17();
        // test18();
        // test19();
        // test20();
        // test21();
        // test22();
        // test23();
        // test24();
        // test25();
        // test26();
        // test27();
        // test28();
        // test29();
        // test30();
        // test31();
        // test32();
        test33();
        // test34();
    }

    private static void test34() {
        AtomicInteger counter = new AtomicInteger(0);
        FunctionalStream.iterateInt(0, 100)
                .finalizeEach(() -> counter.incrementAndGet())
                .forEach(System.out::println);
        System.out.println(counter.get());
    }

    private static void test33() {
        FunctionalStream.iterateLong(0, 10)
                .batch(7)
                .map(longs -> longs.peek(System.out::println))
                .forEach(System.out::println);
        long time = FunctionalStream.iterateLong(0, 1000)
                .batch(700)
                .timeIt();
        System.out.println(time + "ms");

    }

    private static void test32() {
        FunctionalStream.random(new Random(), random -> random.nextInt(10))
                .take(1_000_000_000_000L)
                .sorted()
                .timeIt();
    }

    private static void test31() {
        long time = FunctionalStream.random(new Random(0), random -> random.nextInt(10000))
                .take(10_000_000)
                .sortedViaBuckets() // (100) rnd: 103451ms, 0: 98552ms (10000) 0: > 35 Minuten (unfinished) with optimized flatMap method 4047ms
                // .sorted() // (100) rnd: 1402ms, 0: 1562ms (10000) 0: 1441ms
                // .sortedViaCollections() // (100) rnd: 8359ms, 0: 7283ms (10000) 0: 9771ms
                .timeIt();
        System.out.println(time + "ms");
    }

    private static void test30() {
        FunctionalStream.iterateInt(0, 100)
                .batch(10)
                .forEach(integers -> {
                    System.out.println(integers.scan(Integer::sum).joining(", "));
                });
    }

    private static void test29() {
        FunctionalStream.random(new Random(), random -> random.nextInt(1000000))
                .take(1000000000)
                .sorted()
                .forEach(System.out::println);
    }

    private static void test28() {
        FunctionalStream.iterateInt(0, 100, 2)
                .merge(FunctionalStream.iterateInt(1, 100, 2), Integer::compareTo)
                .forEach(System.out::println);
    }

    private static void test27() {
        FunctionalStream.random(new Random(), random -> random.nextInt(6))
                .take(100000000)
                .map(t -> t + 1)
                .batch(10000)
                .map(functionalStream -> functionalStream
                        .mapWithSizeOfStream((aLong, aLong2) -> (double) aLong / aLong2)
                        .doubleSum()
                )
                .map(d -> Math.round(d * 100000) / 100000.0)
                .scan(Double::sum)
                .zipWithIndex(1L)
                .map(pair -> pair.getK() / pair.getV())
                .map(d -> Math.round(d * 10000000) / 10000000.0)
                .forEach(System.out::println);
    }

    private static void test26() {
        double result = FunctionalStream.random(new Random(), random -> random.nextInt(6))
                .take(100000)
                .map(t -> t + 1)
                .mapWithSizeOfStream((aLong, aLong2) -> (double) aLong / aLong2)
                .doubleSum();
        System.out.println(result);
    }

    private static void test25() {
        FunctionalStream.random(new Random(), random -> random.nextInt(6))
                .take(100000)
                .map(t -> t + 1)
                .makeBuckets(() -> 1L, t -> t + 1L)
                .map(Pair::of)
                .map(pair -> pair.getK() * pair.getV())
                .mapWithSizeOfStream((aLong, aLong2) -> aLong / aLong2)
                .forEach(System.out::println);
    }

    private static void test24() {
        FunctionalStream.random(new Random(), random -> random.nextInt(100))
                .take(10)
                .sorted()
                .reverse()
                .sorted()
                .forEach(System.out::println);
    }

    private static void test23() {
        double d = FunctionalStream.random(new Random(), Random::nextGaussian)
                .limit(1000000)
                .max()
                .orElse(0.0D);
        System.out.println("L: " + d);
    }

    private static void test22() {
        String s =
                "Opmrki wirKx wgLey eyj zsv ampHiv nekh caLs\n" +
                "Mir osaQ osQq oSqa iamk eglxwegIv ephis osag\n" +
                "aiRhmk oigo wglsIr nyrk cSls nE WeifiP avai\n" +
                "wglQmik hew Ivd wglsr iRhixI niHi nekhp wx\n" +
                "amppMk ryr hirR vylIrh hspgl yrh wTiiv\n" +
                "ryv oimr Ostjdivfviglir As yrRyixd";
        List<String> strings = FunctionalStream.of(s)
                .map(current -> current.split("\n"))
                .flatStreamMap(Arrays::stream)
                .map(string -> {
                    return FunctionalStream.of(string)
                            .map(String::chars)
                            .flatStreamMap(Function.identity())
                            .map(i -> (char) (int) i)
                            .indexFilter(l -> (l + 1) % 5 == 0)
                            .map(current -> current + "")
                            .collect(Collectors.joining(""));
                })
                .toList();
        int length = strings.stream()
                .map(String::length)
                .max(Long::compare)
                .orElse(0);
        FunctionalStream.generate(l -> l < length, () -> strings)
                .indexMap((list, l) -> {
                    return FunctionalStream.of(list)
                            .forkingMap(
                                    current -> current.length() <= l,
                                    current -> ' ',
                                    current -> current.charAt((int) (long) l)
                            )
                            .takeWhile(c -> c >= 'a' && c <= 'z')
                            .joining();
                })
                .forEach(System.out::println);
    }

    private static void test21() {
        FunctionalStream.random(new Random(), random -> random.nextInt(10))
                .distinct()
                .take(10)
                .forEach(System.out::println);
    }

    private static void test20() {
        List<String> s = new ArrayList<>();
        s.add("aa");
        s.add("bb");
        s.add("cc");
        s.add("dd");
        s.add("ee");
        s.add("ff");
        s.add("gg");
        s.add("hh");
        s.add("ii");
        FunctionalStream.of(s)
                .forkingMap(t -> Math.random() >= 0.5, String::toUpperCase, String::toLowerCase)
                .map(t -> t.split(""))
                // .peek(t -> System.out.println(Arrays.toString(t)))
                .flatStreamMap(Arrays::stream)
                .distinct()
                .forEach(System.out::println);
    }

    private static void test19() {
        FunctionalStream.generate(Math::random)
                .map(d -> d * 10)
                .map(Double::intValue)
                .distinct()
                .take(6)
                .forEach(System.out::println);
    }

    private static void test18() {
        FunctionalStream.iterateLong(1, 100000000L)
                .scan(Long::sum)
                // .forEach(System.out::println);
                .eval();
    }

    private static void test17() {
        FunctionalStream.iterateLong(1, 21)
                .scan((a, b) -> a * b)
                .forEach(System.out::println);
    }

    private static void test16() {
        FunctionalStream.iterateLong(0, 2)
                .peek(l -> System.out.println("peek: " + l))
                .flatMap(l -> FunctionalStream.iterateLong(l, l + 2))
                .filter(l -> l % 2 == 0)
                .forEach(System.out::println);
    }

    private static void test15() {
        Set<Long> distinctionSet = new HashSet<>();
        FunctionalStream.iterateLong(0, 10)
                .map(l -> l * 2)
                .distinct(distinctionSet)
                .map(l -> l * 2)
                .distinct(distinctionSet)
                .forEach(System.out::println);
    }

    private static void test14() {
        FunctionalStream.iterate(1L, l -> l != 0, l -> l * 2)
                .forEach(System.out::println);
    }

    private static void test13() {
        boolean result = FunctionalStream.iterateLong(0, 1)
                .tap(stream -> stream)
                .anyMatch(l -> (long) l > 0);
        System.out.println(result);
    }

    private static void test12() {
        AtomicReference<Sink<Long>> longSink = new AtomicReference<>(null);
        FunctionalStream.iterateLong(0, 4)
                .insert(longSink::set, l -> l % 2 == 0)
                .peek(l -> longSink.get().accept(l - 1))
                .forEach(System.out::println);
    }

    private static void test11() {
        FunctionalStream.iterateLong(0, 1000)
                .map(l -> l * 3)
                .filter(l -> l % 2 == 0)
                .forEach(System.out::println);
    }

    private static void test10() {
        long factorial = FunctionalStream.iterateLong(1, 11)
                .longMultiplication(Function.identity());
        System.out.println(factorial);
    }

    private static void test9() {
        List<Long> longs = new ArrayList<>();
        longs.add(10000000L);

        AtomicReference<Sink<Long>> longSink = new AtomicReference<>(null);
        long factorial = FunctionalStream.of(longs)
                .insert(longSink::set, l -> l > 0)
                .peek(l -> longSink.get().accept(l - 1))
                .longMultiplication(Function.identity());
        System.out.println(factorial);
    }

    private static void test8() {
        List<String> strings = new ArrayList<>();
        strings.add("Hello World");
        strings.add("Hello World2");
        strings.add("Hello World3");
        strings.add("Hello World4");
        strings.add("Hello World5");
        FunctionalStream.ofWithoutComodification(strings)
                .forEach(System.out::println);
    }

    private static void test7() {
        AtomicReference<Sink<Long>> longSink = new AtomicReference<>(null);
        FunctionalStream.iterateLong(1, 100)
                .inline(System.out::println)
                .peek(System.out::println)
                .insert(longSink::set)
                .forkingMap(l -> l % 2 == 0, l -> l / 2, l -> l * 3 + 1)
                .inline(() -> System.out.println("1"), l -> l == 1)
                .filter(l -> l != 1)
                .peek(l -> longSink.get().accept(l))
                .forEach(System.out::println);
    }

    private static void test6() {
        AtomicReference<Sink<Long>> longSink = new AtomicReference<>(null);
        FunctionalStream.of(1L)
                .insert(longSink::set, l -> l < 10000 && l > 0)
                .distinct(longs -> {
                    List<Long> longList = new ArrayList<>(longs);
                    longList.sort(Long::compareTo);
                    longList.forEach(System.out::println);
                })
                .peek(l -> {
                    longSink.get().accept(l * 2);
                    double d = (l - 1) / 3.0;
                    if ((long) d == d) {
                        longSink.get().accept((long) d);
                    }
                })
                .eval();
        // .forEach(System.out::println);
    }

    private static void test5() {
        FunctionalStream.iterateLong(1, 100)
                .forkingMap(l -> l % 5 == 0 || l % 3 == 0, l -> (l % 3 == 0 ? "Fizz" : "") + (l % 5 == 0 ? "Buzz" : ""), Object::toString)
                .forEach(System.out::println);
    }

    private static void test4() {
        for (String string : FunctionalStream.iterate(1L, l -> l < 500, l -> l + 1)
                .map(l -> {
                    if (l % 15 == 0) return "FizzBuzz";
                    if (l % 5 == 0) return "Buzz";
                    if (l % 3 == 0) return "Fizz";
                    return l + "";
                })) {
            System.out.println(string);
        }
    }

    private static void test3() {
        FunctionalStream.iterate(1L, l -> l < 500, l -> l + 1)
                .map(l -> {
                    if (l % 15 == 0) return "FizzBuzz";
                    if (l % 5 == 0) return "Buzz";
                    if (l % 3 == 0) return "Fizz";
                    return l + "";
                })
                .forEach(System.out::println);
    }

    private static void test2() {
        AtomicReference<Sink<Long>> longSink = new AtomicReference<>(null);
        FunctionalStream.of(1L)
                .insert(longSink::set, l -> l < 1000)
                .peek(l -> longSink.get().accept(l + 1))
                .map(l -> {
                    if (l % 15 == 0) return "FizzBuzz";
                    if (l % 5 == 0) return "Buzz";
                    if (l % 3 == 0) return "Fizz";
                    return l + "";
                })
                .forEach(System.out::println);
    }

    private static void test1() {
        List<String> stringList = new ArrayList<>();
        stringList.add("Hello World");
        stringList.add("Hello World2");
        stringList.add("Hello World3");
        stringList.add("Hello World4");
        stringList.add(null);
        List<String> stringListEmpty = new ArrayList<>();
        stringListEmpty.add("HUGO");
        stringListEmpty.add("Hello World10");
        stringListEmpty.add(null);

        boolean result = FunctionalStream.of(stringListEmpty)
                .concat(FunctionalStream.of(stringList)
                        .map(s -> "T" + s)
                        .concat(stringList)
                        .map(s -> "T" + s)
                        .concat(stringListEmpty))
                .dropNull()
                .filter(s -> !s.startsWith("T"))
                .peek(System.out::println)
                .noneMatch(t -> t.startsWith("H"));
        System.out.println(result);
    }

}
