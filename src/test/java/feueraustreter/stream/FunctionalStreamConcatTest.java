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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
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
        assertThat(result, is(Arrays.asList("1", "2", "3", "4", "5")));
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
        assertThat(result, is(Arrays.asList("4", "5")));
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
        assertThat(result, is(Arrays.asList("1", "2", "3")));
    }

}
