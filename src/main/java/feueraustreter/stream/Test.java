package feueraustreter.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class Test {

    public static void main(String[] args) {
        List<String> stringList = new ArrayList<>();
        stringList.add("Hello World");
        stringList.add("Hello World2");
        stringList.add("Hello World3");
        stringList.add("Hello World4");
        List<String> stringListEmpty = new ArrayList<>();
        stringListEmpty.add("HUGO");
        stringListEmpty.add("Hello World10");

        FunctionalStream.of(stringListEmpty)
                .concat(FunctionalStream.of(stringList)
                        .map(s -> "T" + s)
                        .concat(FunctionalStream.of(stringList))
                        .map(s -> "T" + s)
                        .concat(FunctionalStream.of(stringListEmpty)))
                .filter(s -> !s.equals("THello World"))
                .higherOrderMapWithPrevious("0", s -> {
                    return s1 -> s1.substring(new Random(s.length()).nextInt(s1.length()));
                })
                .peek(System.out::println)
                .eval();
    }

}
