package feueraustreter.stream;

import lombok.NonNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

public class FunctionalStreamImpl<T> implements FunctionalStream<T> {

    private boolean shortCircuit = false;
    private FunctionalStreamImpl<?> root;
    private Sink<T> downstream = null;
    private Iterator<T> streamSource = null;

    protected FunctionalStreamImpl(FunctionalStreamImpl<?> root) {
        this.root = root;
    }

    public FunctionalStreamImpl(@NonNull Iterator<T> streamSource) {
        this.streamSource = streamSource;
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
    public <K> FunctionalStream<K> tap(Function<FunctionalStream<T>, FunctionalStream<K>> tappingFunction) {
        return tappingFunction.apply(this);
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
        eval(t -> {});
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
        shortCircuit = true;
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

    private void eval(Sink<T> sink) {
        downstream = sink;
        root.rootEval();
    }

    private void rootEval() {
        while (streamSource.hasNext()) {
            downstream.accept(streamSource.next());
            if (shortCircuit) {
                return;
            }
        }
    }
}
