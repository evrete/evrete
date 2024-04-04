/**
 * This module provides implementations of Annotated Java Rules - an extension that allows
 * turning Java classes, sources, or JAR archives into rules via simple annotations.
 */
module org.evrete.dsl.java {
    requires org.evrete.core;
    requires java.logging;

    exports org.evrete.dsl.annotation;
    exports org.evrete.dsl;

    provides org.evrete.api.spi.DSLKnowledgeProvider
            with
                    org.evrete.dsl.DSLSourceProvider,
                    org.evrete.dsl.DSLClassProvider,
                    org.evrete.dsl.DSLJarProvider
            ;
}
