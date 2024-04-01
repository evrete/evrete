module org.evrete.core {
    requires java.logging;
    requires jdk.compiler;
    exports org.evrete;
    exports org.evrete.api;
    exports org.evrete.api.builders;
    exports org.evrete.api.annotations;
    exports org.evrete.api.spi;
    exports org.evrete.util;

    uses org.evrete.api.spi.MemoryFactoryProvider;
    provides org.evrete.api.spi.MemoryFactoryProvider with org.evrete.spi.minimal.DefaultMemoryFactoryProvider;
    uses org.evrete.api.spi.ExpressionResolverProvider;
    provides org.evrete.api.spi.ExpressionResolverProvider with org.evrete.spi.minimal.DefaultExpressionResolverProvider;
    uses org.evrete.api.spi.TypeResolverProvider;
    provides org.evrete.api.spi.TypeResolverProvider with org.evrete.spi.minimal.DefaultTypeResolverProvider;
    uses org.evrete.api.spi.LiteralSourceCompiler;
    provides org.evrete.api.spi.LiteralSourceCompiler with org.evrete.spi.minimal.DefaultLiteralSourceCompiler;
    uses org.evrete.api.spi.DSLKnowledgeProvider;
}
