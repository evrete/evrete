/**
 *  This module defines the core API of the Evrete Rule Engine.
 */
module org.evrete.core {
    requires java.logging;
    requires java.compiler;
    exports org.evrete;
    exports org.evrete.api;
    exports org.evrete.api.builders;
    exports org.evrete.api.annotations;
    exports org.evrete.api.events;
    exports org.evrete.api.spi;
    exports org.evrete.util;

    uses org.evrete.api.spi.MemoryFactoryProvider;
    provides org.evrete.api.spi.MemoryFactoryProvider with org.evrete.spi.minimal.DefaultMemoryFactoryProvider;
    uses org.evrete.api.spi.TypeResolverProvider;
    provides org.evrete.api.spi.TypeResolverProvider with org.evrete.spi.minimal.DefaultTypeResolverProvider;
    uses org.evrete.api.spi.SourceCompilerProvider;
    provides org.evrete.api.spi.SourceCompilerProvider with org.evrete.spi.minimal.compiler.DefaultSourceCompilerProvider;
    uses org.evrete.api.spi.DSLKnowledgeProvider;
}
