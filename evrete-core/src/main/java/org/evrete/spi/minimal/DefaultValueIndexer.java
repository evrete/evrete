package org.evrete.spi.minimal;

import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;
import org.evrete.api.spi.ValueIndexer;
import org.evrete.collections.LongKeyMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class DefaultValueIndexer<T> implements ValueIndexer<T> {
    private static final Logger LOGGER = Logger.getLogger(DefaultValueIndexer.class.getName());
    private final ConcurrentHashMap<T, Long> valueToLong = new ConcurrentHashMap<>();
    private final LongKeyMap<T> longToValue = new LongKeyMap<>();
    private final AtomicLong counter = new AtomicLong();

    @Override
    public long getOrCreateId(@NonNull T value) {
        return valueToLong.computeIfAbsent(value, k -> {
            long id = counter.getAndIncrement();
            if(longToValue.put(id, value) == null) {
                return id;
            } else {
                throw new IllegalStateException("Value already exists: " + value + " : " + valueToLong + " : " + longToValue);
            }
        });
    }

    @Nullable
    @Override
    public T get(long id) {
        return longToValue.get(id);
    }

    @Nullable
    @Override
    public synchronized T delete(long id) {
        T found = longToValue.remove(id);
        if(found != null) {
            valueToLong.remove(found, id);
        }
        return found;
    }

    @Override
    public void assignId(long id, @NonNull T value) {
        synchronized (this) {
            longToValue.put(id, value);
            valueToLong.put(value, id);
            // Making sure the counter is properly advanced
            this.counter.updateAndGet(operand -> Math.max(operand, id + 1));
        }
    }

    @Override
    public void clear() {
        this.longToValue.clear();
        this.valueToLong.clear();
        this.counter.set(0);
    }

    ConcurrentHashMap<T, Long> getValueToLong() {
        return valueToLong;
    }

    LongKeyMap<T> getLongToValue() {
        return longToValue;
    }

    AtomicLong getCounter() {
        return counter;
    }
}
