package feueraustreter.stream;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class FunctionalStreamImpl<T> implements FunctionalStream<T> {

    private FunctionalStreamImpl<?> root;
    private Sink<T> downstream = null;
    private Iterable<T> streamSource = null;

    protected FunctionalStreamImpl(FunctionalStreamImpl<?> root) {
        this.root = root;
    }

    public FunctionalStreamImpl(@NonNull Iterable<T> streamSource) {
        this.streamSource = streamSource;
        this.root = this;
    }

    @Override
    public <K> FunctionalStream<K> map(Function<T, K> mapper) {
        FunctionalStreamImpl<K> functionalStream = new FunctionalStreamImpl<>(root);
        downstream = new Sink<T>() {
            @Override
            public void accept(T t) {
                functionalStream.downstream.accept(mapper.apply(t));
            }
        };
        return functionalStream;
    }

    @Override
    public FunctionalStream<T> filter(Predicate<T> filter) {
        FunctionalStreamImpl<T> functionalStream = new FunctionalStreamImpl<>(root);
        downstream = new Sink<T>() {
            @Override
            public void accept(T t) {
                if (filter.test(t)) {
                    functionalStream.downstream.accept(t);
                }
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
        downstream = new Sink<T>() {
            @Override
            public void accept(T t) {
                consumer.accept(t);
                functionalStream.downstream.accept(t);
            }
        };
        return functionalStream;
    }

    @Override
    public void forEach(Consumer<T> consumer) {
        downstream = new Sink<T>() {
            @Override
            public void accept(T t) {
                consumer.accept(t);
            }
        };
        eval();
    }

    @Override
    public List<T> toList() {
        List<T> list = new ArrayList<>();
        downstream = new Sink<T>() {
            @Override
            public void accept(T t) {
                list.add(t);
            }
        };
        eval();
        return list;
    }

    @Override
    public Set<T> toSet() {
        Set<T> list = new HashSet<>();
        downstream = new Sink<T>() {
            @Override
            public void accept(T t) {
                list.add(t);
            }
        };
        eval();
        return list;
    }

    @Override
    public void eval() {
        if (downstream == null) {
            downstream = new Sink<T>() {
                @Override
                public void accept(T t) {
                    // NOOP;
                }
            };
        }
        root.rootEval();
    }

    private void rootEval() {
        streamSource.forEach(downstream);
    }
}
