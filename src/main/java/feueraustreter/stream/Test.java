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
        FunctionalStream.of(stringList).tap(stringFunctionalStream -> {
            return stringFunctionalStream.map(s -> s.substring(1)).map(String::toUpperCase);
        }).peek(System.out::println).eval();
    }

}
