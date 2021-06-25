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

import feueraustreter.lambda.ThrowableFunction;
import lombok.NonNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FunctionalStreamImpl<T> implements FunctionalStream<T> {

    private final FunctionalStreamImpl<?> root;
    private boolean shortCircuit = false;

    private Iterator<T> streamSource = null;
    private Set<FunctionalStream<?>> otherStreamSources = null;
    private Set<FunctionalStream<?>> specialStreamSources = null;

    private Sink<T> downstream = t -> {};

    private Set<Runnable> onClose = new HashSet<>();
    private Set<Runnable> onFinish = new HashSet<>();

    protected FunctionalStreamImpl(FunctionalStreamImpl<?> root) {
        this.root = root;
    }

    public FunctionalStreamImpl(@NonNull Iterator<T> streamSource) {
        this.streamSource = streamSource;
        this.otherStreamSources = new HashSet<>();
        specialStreamSources = new HashSet<>();
        this.root = this;
    }

    @Override
    public <K> FunctionalStream<K> map(Function<? super T, K> mapper) {
        FunctionalStreamImpl<K> functionalStream = new FunctionalStreamImpl<>(root);
        downstream = t -> functionalStream.downstream.accept(mapper.apply(t));
        return functionalStream;
    }

    @Override
    public <K> FunctionalStream<K> flatMap(Function<? super T, FunctionalStream<K>> mapper) {
        FunctionalStreamImpl<K> functionalStream = new FunctionalStreamImpl<>(root);
        downstream = t -> {
            FunctionalStream<K> current = mapper.apply(t);
            current.forEach(k -> {
                functionalStream.downstream.accept(k);
                if (root.shortCircuit) {
                    current.close();
                }
            });
        };
        return functionalStream;
    }

    @Override
    public <K, E extends Throwable> FunctionalStream<K> mapFilter(ThrowableFunction<T, E, K> throwableFunction) {
        FunctionalStreamImpl<K> functionalStream = new FunctionalStreamImpl<>(root);
        downstream = t -> {
            try {
                functionalStream.downstream.accept(throwableFunction.apply(t));
            } catch (Throwable e) {
                // Ignored
            }
        };
        return functionalStream;
    }

    @Override
    public FunctionalStream<T> filter(Predicate<? super T> filter) {
        FunctionalStreamImpl<T> functionalStream = new FunctionalStreamImpl<>(root);
        downstream = t -> {
            if (filter.test(t)) {
                functionalStream.downstream.accept(t);
            }
        };
        return functionalStream;
    }

    @Override
    public Iterator<T> iterator() {
        AtomicReference<Optional<T>> atomicReference = new AtomicReference<>();
        atomicReference.set(evalToNextOutput());
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                if (!atomicReference.get().isPresent() || root.shortCircuit) {
                    root.onFinish.forEach(Runnable::run);
                }
                return !root.shortCircuit && atomicReference.get().isPresent();
            }

            @Override
            public T next() {
                Optional<T> current = atomicReference.get();
                atomicReference.set(evalToNextOutput());
                return current.get();
            }
        };
    }

    @Override
    public Stream<T> toStream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public FunctionalStream<T> concat(FunctionalStream<T> other) {
        FunctionalStreamImpl<T> functionalStream = new FunctionalStreamImpl<>(root);
        downstream = t -> functionalStream.downstream.accept(t);
        other = other.peek(t -> functionalStream.downstream.accept(t));
        root.otherStreamSources.add(other);
        return functionalStream;
    }

    @Override
    public FunctionalStream<T> fork(Consumer<FunctionalStream<T>> fork) {
        LinkedList<T> thisStream = new LinkedList<>();
        LinkedList<T> otherStream = new LinkedList<>();

        FunctionalStreamImpl<T> forkedStream = new FunctionalStreamImpl<>(forkedIterator(otherStream));
        FunctionalStreamImpl<T> functionalStream = new FunctionalStreamImpl<>(forkedIterator(thisStream));

        downstream = t -> {
            thisStream.add(t);
            otherStream.add(t);
        };
        fork.accept(forkedStream);
        return functionalStream;
    }

    private Iterator<T> forkedIterator(LinkedList<T> list) {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                while (list.isEmpty() && root.hasNext()) {
                    root.evalNext();
                }
                return !list.isEmpty();
            }

            @Override
            public T next() {
                return list.removeFirst();
            }
        };
    }

    @Override
    public FunctionalStream<T> insert(Consumer<Sink<T>> sink) {
        FunctionalStreamImpl<T> functionalStream = new FunctionalStreamImpl<>(root);
        downstream = t -> functionalStream.downstream.accept(t);

        LinkedList<T> list = new LinkedList<>();
        sink.accept(list::add);
        FunctionalStream<T> other = new FunctionalStreamImpl<>(new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return !list.isEmpty();
            }

            @Override
            public T next() {
                return list.removeFirst();
            }
        });
        other = other.peek(t -> functionalStream.downstream.accept(t));
        root.specialStreamSources.add(other);
        return functionalStream;
    }

    @Override
    public FunctionalStream<T> onClose(Runnable runnable) {
        root.onClose.add(runnable);
        return this;
    }

    @Override
    public FunctionalStream<T> onFinish(Runnable runnable) {
        root.onFinish.add(runnable);
        return this;
    }

    @Override
    public void forEach(Consumer<? super T> consumer) {
        eval(consumer::accept);
    }

    @Override
    public void evalNext() {
        if (downstream == null) {
            downstream = t -> {
            };
        }
        root.evalNextInternal();
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        AtomicBoolean result = new AtomicBoolean(false);
        eval(t -> {
            if (predicate.test(t)) {
                result.set(true);
                close();
            }
        });
        return result.get();
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        AtomicBoolean result = new AtomicBoolean(true);
        eval(t -> {
            if (!predicate.test(t)) {
                result.set(false);
                close();
            }
        });
        return result.get();
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        AtomicBoolean result = new AtomicBoolean(true);
        eval(t -> {
            if (predicate.test(t)) {
                result.set(false);
                close();
            }
        });
        return result.get();
    }

    @Override
    public void close() {
        root.shortCircuit = true;
        root.onClose.forEach(Runnable::run);
    }

    @Override
    public boolean isClosed() {
        return root.shortCircuit;
    }

    @Override
    public Optional<T> findFirst() {
        AtomicReference<Optional<T>> result = new AtomicReference<>(Optional.empty());
        eval(t -> {
            result.set(Optional.of(t));
            close();
        });
        return result.get();
    }

    private void eval(Sink<T> sink) {
        downstream = sink;
        root.evalAll();
    }

    private void evalAll() {
        if (shortCircuit) {
            throw new UnsupportedOperationException("This Stream is already evaluated");
        }
        while (hasNext()) {
            evalNextInternal();
            if (shortCircuit) {
                root.onFinish.forEach(Runnable::run);
                return;
            }
        }
        root.onFinish.forEach(Runnable::run);
    }

    private void evalNextInternal() {
        for (FunctionalStream<?> functionalStream : specialStreamSources) {
            if (functionalStream.hasNext()) {
                functionalStream.evalNext();
                return;
            }
        }
        if (streamSource.hasNext()) {
            downstream.accept(streamSource.next());
            return;
        }
        for (FunctionalStream<?> functionalStream : otherStreamSources) {
            if (functionalStream.hasNext()) {
                functionalStream.evalNext();
                return;
            }
        }
    }

    private Optional<T> evalToNextOutput() {
        AtomicReference<Optional<T>> atomicReference = new AtomicReference<>(Optional.empty());
        downstream = t -> atomicReference.set(Optional.of(t));
        while (hasNext() && !atomicReference.get().isPresent()) {
            evalNext();
            if (root.shortCircuit) {
                break;
            }
        }
        return atomicReference.get();
    }

    @Override
    public boolean hasNext() {
        return root.streamSource.hasNext() || root.otherStreamSources.stream().anyMatch(FunctionalStream::hasNext) || root.specialStreamSources.stream().anyMatch(FunctionalStream::hasNext);
    }
}
