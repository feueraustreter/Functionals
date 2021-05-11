package feueraustreter.stream;

import lombok.NonNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FunctionalStreamImpl<T> implements FunctionalStream<T> {

    private final FunctionalStreamImpl<?> root;
    private Iterator<T> streamSource = null;
    private boolean shortCircuit = false;

    private Set<FunctionalStream<?>> zippedStreams = null;

    private Sink<T> downstream = null;

    protected FunctionalStreamImpl(FunctionalStreamImpl<?> root) {
        this.root = root;
    }

    public FunctionalStreamImpl(@NonNull Iterator<T> streamSource) {
        this.streamSource = streamSource;
        this.zippedStreams = new HashSet<>();
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
    public FunctionalStream<T> partialMap(Predicate<? super T> filter, UnaryOperator<T> mapper) {
        FunctionalStreamImpl<T> functionalStream = new FunctionalStreamImpl<>(root);
        downstream = t -> {
            if (filter.test(t)) {
                functionalStream.downstream.accept(mapper.apply(t));
            } else {
                functionalStream.downstream.accept(t);
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
    public FunctionalStream<T> peek(Consumer<? super T> consumer) {
        FunctionalStreamImpl<T> functionalStream = new FunctionalStreamImpl<>(root);
        downstream = t -> {
            consumer.accept(t);
            functionalStream.downstream.accept(t);
        };
        return functionalStream;
    }

    @Override
    public FunctionalStream<T> limit(long count) {
        FunctionalStreamImpl<T> functionalStream = new FunctionalStreamImpl<>(root);
        downstream = new Sink<T>() {
            int counted = 0;

            @Override
            public void accept(T t) {
                if (counted < count) {
                    functionalStream.downstream.accept(t);
                }
                counted++;
            }
        };
        return functionalStream;
    }

    @Override
    public FunctionalStream<T> skip(long count) {
        FunctionalStreamImpl<T> functionalStream = new FunctionalStreamImpl<>(root);
        downstream = new Sink<T>() {
            int skipped = 0;

            @Override
            public void accept(T t) {
                if (skipped >= count) {
                    functionalStream.downstream.accept(t);
                }
                skipped++;
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
    public FunctionalStream<T> zip(FunctionalStream<T> other) {
        FunctionalStreamImpl<T> functionalStream = new FunctionalStreamImpl<>(root);
        downstream = t -> functionalStream.downstream.accept(t);
        other = other.peek(t -> functionalStream.downstream.accept(t));
        other.close();
        root.zippedStreams.add(other);
        return functionalStream;
    }

    @Override
    public void forEach(Consumer<? super T> consumer) {
        eval(consumer::accept);
    }

    @Override
    public List<T> toList() {
        List<T> list = new ArrayList<>();
        eval(list::add);
        return list;
    }

    @Override
    public Set<T> toSet() {
        Set<T> list = new HashSet<>();
        eval(list::add);
        return list;
    }

    @Override
    public String joining(String delimiter) {
        StringBuilder stringBuilder = new StringBuilder();
        eval(new Sink<T>() {
            boolean first = true;

            @Override
            public void accept(T t) {
                if (!first) {
                    stringBuilder.append(delimiter);
                }
                stringBuilder.append(t.toString());
                first = false;
            }
        });
        return stringBuilder.toString();
    }

    @Override
    public void eval() {
        eval(t -> {
        });
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        AtomicBoolean result = new AtomicBoolean(false);
        eval(t -> {
            if (predicate.test(t)) {
                result.set(true);
                root.shortCircuit = true;
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
                root.shortCircuit = true;
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
                root.shortCircuit = true;
            }
        });
        return result.get();
    }

    @Override
    public long count() {
        AtomicLong result = new AtomicLong(0);
        eval(t -> result.incrementAndGet());
        return result.get();
    }

    @Override
    public void close() {
        root.shortCircuit = true;
    }

    @Override
    public Optional<T> findFirst() {
        AtomicReference<Optional<T>> result = new AtomicReference<>(Optional.empty());
        eval(t -> {
            result.set(Optional.of(t));
            root.shortCircuit = true;
        });
        return result.get();
    }

    @Override
    public Optional<T> min(Comparator<T> comparator) {
        AtomicReference<Optional<T>> result = new AtomicReference<>(Optional.empty());
        eval(t -> {
            Optional<T> current = result.get();
            if (!current.isPresent()) {
                result.set(Optional.of(t));
                return;
            }
            if (comparator.compare(current.get(), t) > 0) {
                result.set(Optional.of(t));
            }
        });
        return result.get();
    }

    @Override
    public Optional<T> max(Comparator<T> comparator) {
        AtomicReference<Optional<T>> result = new AtomicReference<>(Optional.empty());
        eval(t -> {
            Optional<T> current = result.get();
            if (!current.isPresent()) {
                result.set(Optional.of(t));
                return;
            }
            if (comparator.compare(current.get(), t) < 0) {
                result.set(Optional.of(t));
            }
        });
        return result.get();
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        A container = collector.supplier().get();
        BiConsumer<A, ? super T> biConsumer = collector.accumulator();
        eval(t -> biConsumer.accept(container, t));
        return collector.finisher().apply(container);
    }

    @Override
    public T[] toArray(IntFunction<T[]> intFunction) {
        List<T> list = toList();
        return list.toArray(intFunction.apply(list.size()));
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        AtomicReference<T> result = new AtomicReference<>(identity);
        eval(t -> result.set(accumulator.apply(result.get(), t)));
        return result.get();
    }

    private void eval(Sink<T> sink) {
        downstream = sink;
        root.evalAll();
    }

    private void evalAll() {
        while (hasNext()) {
            evalNext();
            if (shortCircuit) {
                return;
            }
        }
    }

    private void evalNext() {
        if (streamSource.hasNext()) {
            downstream.accept(streamSource.next());
            return;
        }
        if (zippedStreams.stream().noneMatch(FunctionalStream::hasNext)) {
            return;
        }
        zippedStreams.stream().filter(FunctionalStream::hasNext).peek(FunctionalStream::eval).findFirst();
    }

    private Optional<T> evalToNextOutput() {
        AtomicReference<Optional<T>> atomicReference = new AtomicReference<>(Optional.empty());
        downstream = t -> atomicReference.set(Optional.of(t));
        while (hasNext() && !atomicReference.get().isPresent()) {
            root.evalNext();
            if (root.shortCircuit) {
                return Optional.empty();
            }
        }
        return atomicReference.get();
    }

    @Override
    public boolean hasNext() {
        return root.streamSource.hasNext() || root.zippedStreams.stream().anyMatch(FunctionalStream::hasNext);
    }
}
