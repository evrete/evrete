package org.evrete.util;

import org.evrete.api.RuleSession;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.stream.Collector.Characteristics.IDENTITY_FINISH;
import static java.util.stream.Collector.Characteristics.UNORDERED;

/**
 * A class that collects/inserts elements into a {@link RuleSession} instance.
 *
 * @param <T> the type of the input elements
 * @param <S> the type of the {@link RuleSession} context
 */
public class SessionCollector<T, S extends RuleSession<S>> implements Collector<T, S, S> {
    private static final Set<Characteristics> CHARACTERISTICS = Collections.unmodifiableSet(
            EnumSet.of(UNORDERED, IDENTITY_FINISH)
    );
    private final S session;

    public SessionCollector(S session) {
        this.session = session;
    }

    @Override
    public Supplier<S> supplier() {
        return () -> session;
    }

    @Override
    public BiConsumer<S, T> accumulator() {
        return (acc, o) -> session.insert(o);
    }

    @Override
    public BinaryOperator<S> combiner() {
        return (acc1, acc2) -> session;
    }

    @Override
    public Function<S, S> finisher() {
        return a -> session;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return CHARACTERISTICS;
    }
}
