package feueraustreter.stream;

import lombok.AllArgsConstructor;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FunctionalStreamImpl<T> implements FunctionalStream<T> {

    private int index = 0;
    private int virtualIndex = 0;
    private AtomicBoolean shortCircuit = new AtomicBoolean(false);
    private Iterator<?> streamSource;
    private List<Object> operations = new ArrayList<>();
    private Set<Runnable> onClose = new HashSet<>();
    private Set<Runnable> onFinish = new HashSet<>();

    protected FunctionalStreamImpl(FunctionalStreamImpl<?> stream) {
        this.index = stream.virtualIndex + 1;
        this.virtualIndex = stream.virtualIndex + 1;
        this.shortCircuit = stream.shortCircuit;
        this.streamSource = stream.streamSource;
        this.operations = stream.operations;
        this.onClose = stream.onClose;
        this.onFinish = stream.onFinish;
    }

    public FunctionalStreamImpl(Iterator<?> streamSource) {
        this.streamSource = streamSource;
    }

    public <K> FunctionalStream<K> map(Function<? super T, K> mapper) {
        boolean shouldCreateNew = operations.isEmpty() || !(operations.get(operations.size() - 1) instanceof Function);
        if (shouldCreateNew) {
            FunctionalStreamImpl<K> result = new FunctionalStreamImpl<>(this);
            result.operations.add(mapper);
            return result;
        } else {
            virtualIndex++;
            operations.add(mapper);
            return (FunctionalStream<K>) this;
        }
    }

    @Override
    public FunctionalStream<T> filter(Predicate<? super T> filter) {
        boolean shouldCreateNew = operations.isEmpty() || !(operations.get(operations.size() - 1) instanceof Predicate);
        if (shouldCreateNew) {
            FunctionalStreamImpl<T> result = new FunctionalStreamImpl<>(this);
            result.operations.add(filter);
            return result;
        } else {
            virtualIndex++;
            operations.add(filter);
            return this;
        }
    }

    @Override
    public Iterator<T> iterator() {
        AtomicReference<T> current = new AtomicReference<>();
        AtomicBoolean hasNext = new AtomicBoolean(false);
        if (hasNext()) {
            try {
                current.set(nextElement());
                hasNext.set(true);
            } catch (NoResultException e) {
                hasNext.set(false);
            }
        }
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return hasNext.get();
            }

            @Override
            public T next() {
                T currentValue = current.get();
                if (FunctionalStreamImpl.this.hasNext()) {
                    try {
                        current.set(FunctionalStreamImpl.this.nextElement());
                        hasNext.set(true);
                    } catch (NoResultException e) {
                        hasNext.set(false);
                    }
                } else {
                    hasNext.set(false);
                }
                return currentValue;
            }
        };
    }

    @Override
    public Spliterator<T> spliterator() {
        return new Spliterator<T>() {
            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                if (hasNext()) {
                    try {
                        action.accept(nextElement());
                        return true;
                    } catch (NoResultException e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            @Override
            public Spliterator<T> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }

            @Override
            public int characteristics() {
                return ORDERED;
            }
        };
    }

    @Override
    public Stream<T> toStream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public FunctionalStream<T> onClose(Runnable runnable) {
        onClose.add(runnable);
        return this;
    }

    @Override
    public FunctionalStream<T> onFinish(Runnable runnable) {
        onFinish.add(runnable);
        return this;
    }

    @Override
    public void forEach(Consumer<? super T> consumer) {
        spliterator().forEachRemaining(consumer);
        onFinish.forEach(Runnable::run);
    }

    @Override
    public void close() {
        shortCircuit.set(true);
        onClose.forEach(Runnable::run);
    }

    @Override
    public boolean isClosed() {
        return shortCircuit.get();
    }

    @Override
    public boolean hasNext() {
        return streamSource.hasNext();
    }

    @Override
    public T nextElement() {
        if (virtualIndex != operations.size()) {
            throw new IllegalStateException("Cannot call nextElement() before all operations have been applied");
        }
        while (true) {
            if (!hasNext() || isClosed()) {
                throw new NoResultException();
            }

            Object object;
            try {
                object = streamSource.next();
            } catch (NoSuchElementException e) {
                throw new NoResultException(e.getMessage(), e);
            }
            Result result = createResult(object, 0, operations.size());
            if (result == null) {
                continue;
            }
            return (T) result.value;
        }
    }

    @AllArgsConstructor
    private static class Result {
        private Object value;
    }

    private static class NoResultException extends RuntimeException {

        public NoResultException() {
            super();
        }

        public NoResultException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @FunctionalInterface
    private interface FlatMapConsumer<T> {
        void accept(T t);
    }

    private Result createResult(Object current, int from, int to) {
        if (from == to) {
            return new Result(current);
        }
        for (int i = from; i < to; i++) {
            Object operation = operations.get(i);
            if (operation instanceof Function) {
                Function function = (Function) operation;
                current = function.apply(current);
            } else if (operation instanceof Predicate) {
                Predicate predicate = (Predicate) operation;
                if (!predicate.test(current)) {
                    return null;
                }
            } else if (operation instanceof FlatMapConsumer) {
                ((FlatMapConsumer) operation).accept(current);
                return null;
            }
        }
        return new Result(current);
    }
}
