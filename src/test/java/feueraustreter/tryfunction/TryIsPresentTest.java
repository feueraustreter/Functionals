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
