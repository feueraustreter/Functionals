package feueraustreter.stream;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class FunctionalStreamConcatTest {

    @Test
    public void testConcat() {
        List<String> stringList = new ArrayList<>();
        stringList.add("1");
        stringList.add("2");
        stringList.add("3");
        List<String> alternativeStringList = new ArrayList<>();
        alternativeStringList.add("4");
        alternativeStringList.add("5");

        List<String> result = FunctionalStream.of(stringList)
                .concat(FunctionalStream.of(alternativeStringList))
                .toList();
        assertThat(result.size(), is(5));
    }

    @Test
    public void testEmptyConcat() {
        List<String> stringList = new ArrayList<>();
        List<String> alternativeStringList = new ArrayList<>();
        alternativeStringList.add("4");
        alternativeStringList.add("5");

        List<String> result = FunctionalStream.of(stringList)
                .concat(FunctionalStream.of(alternativeStringList))
                .toList();
        assertThat(result.size(), is(2));
    }

    @Test
    public void testEmptyConcatOther() {
        List<String> stringList = new ArrayList<>();
        stringList.add("1");
        stringList.add("2");
        stringList.add("3");
        List<String> alternativeStringList = new ArrayList<>();

        List<String> result = FunctionalStream.of(stringList)
                .concat(FunctionalStream.of(alternativeStringList))
                .toList();
        assertThat(result.size(), is(3));
    }

}
