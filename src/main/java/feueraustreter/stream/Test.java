package feueraustreter.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Test {

    public static void main(String[] args) {
        List<String> stringList = new ArrayList<>();
        stringList.add("Hello World");
        stringList.add("Hello World2");
        stringList.add("Hello World3");
        stringList.add("Hello World4");
        String t = FunctionalStream.of(stringList)
                .peek(System.out::println)
                .collect(Collectors.joining());
        System.out.println(t);
    }

}
