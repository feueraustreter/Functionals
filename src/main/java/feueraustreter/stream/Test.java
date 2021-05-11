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
        boolean t = FunctionalStream.of(stringListEmpty)
                .zip(FunctionalStream.of(stringList).map(s -> "T" + s))
                .filter(s -> !s.startsWith("THello World"))
                .map(String::toCharArray)
                // .toStream()
                .peek(System.out::println)
                .map(String::new)
                .anyMatch(s -> s.startsWith("T"));
        System.out.println(t);

        for (String s : FunctionalStream.of(stringList)
                .map(h -> h + " " + h)) {
            System.out.println(s);
        }
    }

}
