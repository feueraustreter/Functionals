package feueraustreter.stream;

import lombok.NonNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class FunctionalStreamImpl<T> implements FunctionalStream<T> {

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
    public <K> FunctionalStream<K> map(Function<T, K> mapper) {
        FunctionalStreamImpl<K> functionalStream = new FunctionalStreamImpl<>(root);
        downstream = t -> functionalStream.downstream.accept(mapper.apply(t));
        return functionalStream;
    }

    @Override
    public <K> FunctionalStream<K> flatMap(Function<T, FunctionalStream<K>> mapper) {
        FunctionalStreamImpl<K> functionalStream = new FunctionalStreamImpl<>(root);
        downstream = t -> mapper.apply(t).forEach(functionalStream.downstream);
        return functionalStream;
    }

    @Override
    public FunctionalStream<T> filter(Predicate<T> filter) {
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
    public FunctionalStream<T> peek(Consumer<T> consumer) {
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
    public void forEach(Consumer<T> consumer) {
        downstream = consumer::accept;
        eval();
    }

    @Override
    public List<T> toList() {
        List<T> list = new ArrayList<>();
        downstream = list::add;
        eval();
        return list;
    }

    @Override
    public Set<T> toSet() {
        Set<T> list = new HashSet<>();
        downstream = list::add;
        eval();
        return list;
    }

    @Override
    public String joining(String delimiter) {
        StringBuilder stringBuilder = new StringBuilder();
        downstream = new Sink<T>() {
            boolean first = true;

            @Override
            public void accept(T t) {
                if (!first) {
                    stringBuilder.append(delimiter);
                }
                stringBuilder.append(t.toString());
                first = false;
            }
        };
        eval();
        return stringBuilder.toString();
    }

    @Override
    public void eval() {
        if (downstream == null) {
            downstream = t -> {};
        }
        root.rootEval();
    }

    private void rootEval() {
        streamSource.forEachRemaining(downstream);
    }
}
