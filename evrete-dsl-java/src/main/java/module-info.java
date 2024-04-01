module org.evrete.dsl.java {
    requires org.evrete.core;
    requires java.logging;

    provides org.evrete.api.spi.DSLKnowledgeProvider
            with
                    org.evrete.dsl.DSLSourceProvider,
                    org.evrete.dsl.DSLClassProvider,
                    org.evrete.dsl.DSLJarProvider
            ;
}
