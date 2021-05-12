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

public class FunctionalStreamFilterTest {

    @Test
    public void testFilter() {
        List<String> stringList = new ArrayList<>();
        stringList.add("1");
        stringList.add("2");
        stringList.add("3");

        List<String> result = FunctionalStream.of(stringList)
                .filter(s -> s.equals("2"))
                .toList();
        assertThat(result.size(), is(1));
        assertThat(result, is(Arrays.asList("2")));
    }

    @Test
    public void testRemoveAll() {
        List<String> stringList = new ArrayList<>();
        stringList.add("1");
        stringList.add("2");
        stringList.add("3");

        List<String> result = FunctionalStream.of(stringList)
                .removeAll(s -> s.equals("2"))
                .toList();
        assertThat(result.size(), is(2));
        assertThat(result, is(Arrays.asList("1", "3")));
    }

}
