package feueraustreter.stream;

import java.util.ArrayList;
import java.util.List;

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
                .zip(FunctionalStream.of(stringList)
                        .map(s -> "T" + s)
                        .zip(FunctionalStream.of(stringList))
                        .map(s -> "T" + s)
                        .zip(FunctionalStream.of(stringListEmpty)))
                .filter(s -> !s.equals("THello World"))
                .peek(System.out::println)
                .eval();
    }

}
