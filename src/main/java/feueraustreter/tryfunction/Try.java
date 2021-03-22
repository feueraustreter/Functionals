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

@Getter
@SuppressWarnings("squid:S00100") // Method name
public class Try<V, E extends Throwable> {

    private final V success;
    private final E failure;

    public Try(V success, E failure) {
        this.success = success;
        this.failure = failure;
    }

    public static <V, E extends Throwable> Try<V, E> Success(V value) {
        return new Try<>(value, null);
    }

    public static <V, E extends Throwable> Try<V, E> Failure(E exception) {
        return new Try<>(null, exception);
    }

    public boolean successful() {
        return failure == null;
    }

    public boolean failed() {
        return failure != null;
    }

    @SuppressWarnings({"java:S1181" /* Catch exception */, "unchecked"})
    public static <V, E extends Throwable> Try<V, E> tryIt(TryFunction<V, E> f) {
        try {
            return Try.Success(f.f());
        } catch (Throwable e) {
            return Try.Failure((E) e);
        }
    }

    public interface TryFunction<V, E extends Throwable> {

        V f() throws E;

    }

}
