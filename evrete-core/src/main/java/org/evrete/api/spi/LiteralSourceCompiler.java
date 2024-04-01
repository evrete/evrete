package org.evrete.api.spi;

import org.evrete.api.*;
import org.evrete.util.CompilationException;

import java.util.Collection;


/**
 * An interface that represents a Service Provider Interface (SPI) for compiling rule's literal sources.
 */
public interface LiteralSourceCompiler extends OrderedServiceProvider {

    /**
     * Compiles a collection of rule literal sources into a collection of compiled rule sources.
     *
     * @param <S> the type of source data, which is a subtype of RuleLiteralData containing rule data
     * @param <R> the type of rule, which is a subtype of Rule that the source data is associated with
     * @param context the runtime context in which the compilation occurs
     * @param sources the collection of source data to compile
     * @return a collection of compiled rule sources
     * @throws CompilationException if any compilation error occurs
     */
    <S extends RuleLiteralData<R>, R extends Rule> Collection<RuleCompiledSources<S, R>> compile(RuntimeContext<?> context, Collection<S> sources) throws CompilationException;
}
