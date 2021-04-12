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

import lombok.Getter;
import lombok.NonNull;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Stream;

/**
 * The Try class is a wrapper around a value or an exception. This can be useful in situations
 * where you want to map some data to another in a {@link Stream}. To construct a Try instance
 * you can either use {@link #Success(Object)} for a Try instance which is considered
 * successful or {@link #Failure(Throwable)} for a Try instance which is considered a failure.
 * You can also create a Try instance by using {@link #tryIt(TryFunction)}. The
 * {@link TryFunction} is used to either return a value or throw an Exception which is caught
 * in the {@link #tryIt(TryFunction)} method, which will then either return
 * {@link #Success(Object)} or {@link #Failure(Throwable)}.
 *
 * @param <V> the success type to use
 * @param <E> the failure type to use
 */
@Getter
@SuppressWarnings("squid:S00100") // Method name
public class Try<V, E extends Throwable> {

    private final V success;
    private final E failure;

    private Try(V success, E failure) {
        this.success = success;
        this.failure = failure;
    }

    /**
     * Create a Try instance that was successful with a given value.
     *
     * @param value the success value
     * @param <V> the success type to use
     * @param <E> the failure type to use
     * @return the Try instance containing the value as a success
     */
    public static <V, E extends Throwable> Try<V, E> Success(V value) {
        return new Try<>(value, null);
    }

    /**
     * Create a Try instance that was failed with a given exception.
     *
     * @param exception the exception value
     * @param <V> the success type to use
     * @param <E> the failure type to use
     * @return the Try instance containing the exception as a failure
     */
    public static <V, E extends Throwable> Try<V, E> Failure(@NonNull E exception) {
        return new Try<>(null, exception);
    }

    /**
     * Check if the Try was successful or failed.
     *
     * @return {@code true} if and only if {@link #failure} is null, {@code false} otherwise
     */
    public boolean successful() {
        return failure == null;
    }

    /**
     * Check if the Try failed or was successful.
     *
     * @return {@code true} if and only if {@link #failure} is not null, {@code false} otherwise
     */
    public boolean failed() {
        return failure != null;
    }

    /**
     * Check if the Try is successful and {@link #success} is not null.
     *
     * @return {@code true} if and only is {@link #success} is not null, {@code false} otherwise
     */
    public boolean hasValue() {
        return success != null;
    }

    /**
     * Check if the Try is successful or delegate to Optional, OptionalInt, OptionalLong or
     * OptionalDouble. To be exact it will check if the Try was not a failure and return
     * the delegated next.
     *
     * @return {@code true} if success is null or {@link Optional#isPresent()}, {@link OptionalInt#isPresent()}, {@link OptionalLong#isPresent()} or {@link OptionalDouble#isPresent()} returns true, {@code false} if and only if {@link #failure} is not null
     */
    public boolean isPresent() {
        if (failure != null) {
            return false;
        }
        if (success == null) {
            return true;
        }
        if (success instanceof Optional<?>) {
            return ((Optional<?>) success).isPresent();
        }
        if (success instanceof OptionalInt) {
            return ((OptionalInt) success).isPresent();
        }
        if (success instanceof OptionalLong) {
            return ((OptionalLong) success).isPresent();
        }
        if (success instanceof OptionalDouble) {
            return ((OptionalDouble) success).isPresent();
        }
        return true;
    }

    /**
     * This method is used in conjunction with the {@link TryFunction} interface to either
     * return a successful Try or failed Try instance.
     *
     * @param f the try function to use
     * @param <V> the success type to use
     * @param <E> the failure type to use
     * @return either a successful Try instance or a failed one
     */
    @SuppressWarnings({"java:S1181" /* Catch exception */, "unchecked"})
    public static <V, E extends Throwable> Try<V, E> tryIt(TryFunction<V, E> f) {
        try {
            return Try.Success(f.f());
        } catch (Throwable e) {
            return Try.Failure((E) e);
        }
    }

    /**
     * This interface is used in the {@link #tryIt(TryFunction)} method to either return an
     * Object or throw an Exception. The {@link #tryIt(TryFunction)} method will wrap the
     * return value from the method {@link #f()} to either a successful Try instance or a
     * failed Try instance, by calling {@link #Success(Object)} or {@link #Failure(Throwable)}
     * respectively. Returning null from {@link #f()} is considered {@link #successful()}.
     * But will return {@code false} for {@link #hasValue()}. If you return {@link Optional},
     * {@link OptionalInt}, {@link OptionalLong} or {@link OptionalDouble} the
     * {@link #isPresent()} method calls will be delegated to the optional itself. By using
     * this interface in conjunction with the {@link #tryIt(TryFunction)} method the
     * Exception type will be lost, if not explicitly casting the {@link TryFunction} to the
     * desired type.
     *
     * @param <V> the success type to use
     * @param <E> the failure type to use
     */
    @FunctionalInterface
    public interface TryFunction<V, E extends Throwable> {
        V f() throws E;
    }

}
