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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Test {

    public static void main(String[] args) {
        // test1();
        test2();
        // test5();
    }

    private static void test5() {
        FunctionalStream.iterateLong(1, 100)
                .forkingMap(l -> l % 5 == 0 || l % 3 == 0, l -> (l % 3 == 0 ? "Fizz" : "") + (l % 5 == 0 ? "Buzz" : ""), Object::toString)
                .forEach(System.out::println);
                // .eval();
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
                .filteredInsert(longSink::set, l -> l < 1000)
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
