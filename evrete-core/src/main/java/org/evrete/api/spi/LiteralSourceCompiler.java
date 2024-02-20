package org.evrete.api.spi;

import org.evrete.api.*;
import org.evrete.runtime.compiler.CompilationException;

import java.util.Collection;


public interface LiteralSourceCompiler extends OrderedServiceProvider {

    <S extends RuleLiteralSources<R>, R extends Rule> Collection<RuleCompiledSources<S, R>> compile(RuntimeContext<?> context, Collection<S> sources) throws CompilationException;
}
