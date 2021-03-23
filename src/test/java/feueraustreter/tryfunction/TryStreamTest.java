package feueraustreter.tryfunction;

import feueraustreter.tryfunction.sample.User;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TryStreamTest {

    @Test
    public void testSuccessful() {
        Try<User, RuntimeException> successfulTry = Try.Success(new User("foo", false));
        assertThat(successfulTry.successful(), is(true));
    }

    @Test
    public void testSuccessfulNull() {
        Try<User, RuntimeException> successfulTry = Try.Success(null);
        assertThat(successfulTry.successful(), is(true));
    }

    @Test
    public void testFailureFalseOnSuccessful() {
        Try<User, RuntimeException> failureTry = Try.Failure(new RuntimeException());
        assertThat(failureTry.successful(), is(false));
    }

    @Test
    public void testFailure() {
        Try<User, RuntimeException> failureTry = Try.Failure(new RuntimeException());
        assertThat(failureTry.failed(), is(true));
    }

    @Test
    public void testSuccessfulFalseOnFailure() {
        Try<User, RuntimeException> failureTry = Try.Failure(new RuntimeException());
        assertThat(failureTry.successful(), is(false));
    }

    @Test
    public void testHasValue() {
        Try<User, RuntimeException> successfulTry = Try.Success(new User("foo", false));
        assertThat(successfulTry.hasValue(), is(true));
    }

    @Test
    public void testHasValueNull() {
        Try<User, RuntimeException> successfulTry = Try.Success(null);
        assertThat(successfulTry.hasValue(), is(false));
    }

    @Test
    public void testHasValueForFailure() {
        Try<User, RuntimeException> failureTry = Try.Failure(new RuntimeException());
        assertThat(failureTry.hasValue(), is(false));
    }

}
