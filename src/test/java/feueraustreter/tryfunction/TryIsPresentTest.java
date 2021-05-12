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

package feueraustreter.tryfunction;

import feueraustreter.tryfunction.sample.User;
import org.junit.Test;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TryIsPresentTest {

    @Test
    public void testIsPresentForFailure() {
        Try<Optional<User>, RuntimeException> optionalTry = Try.Failure(new RuntimeException());
        assertThat(optionalTry.isPresent(), is(false));
    }

    @Test
    public void testIsPresentForNullSuccess() {
        Try<Optional<User>, RuntimeException> optionalTry = Try.Success(null);
        assertThat(optionalTry.isPresent(), is(true));
    }

    @Test
    public void testIsPresentForSuccess() {
        Try<User, RuntimeException> optionalTry = Try.Success(new User("foo", false));
        assertThat(optionalTry.isPresent(), is(true));
    }

    @Test
    public void testOptionalIsNotPresentCheck() {
        Try<Optional<User>, RuntimeException> optionalTry = Try.Success(Optional.empty());
        assertThat(optionalTry.isPresent(), is(false));
    }

    @Test
    public void testOptionalIsPresentCheck() {
        Try<Optional<User>, RuntimeException> optionalTry = Try.Success(Optional.of(new User("foo", true)));
        assertThat(optionalTry.isPresent(), is(true));
    }

    @Test
    public void testOtherOptionalIsPresentChecks() {
        Try<OptionalInt, RuntimeException> optionalIntTry = Try.Success(OptionalInt.of(0));
        assertThat(optionalIntTry.isPresent(), is(true));

        Try<OptionalLong, RuntimeException> optionalLongTry = Try.Success(OptionalLong.of(0));
        assertThat(optionalLongTry.isPresent(), is(true));

        Try<OptionalDouble, RuntimeException> optionalDoubleTry = Try.Success(OptionalDouble.of(0));
        assertThat(optionalDoubleTry.isPresent(), is(true));
    }

}
