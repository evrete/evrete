package org.evrete.api;

public interface RuntimeRule extends Rule {

    RuntimeRule addImport(String imp);

    KnowledgeSession<?> getRuntime();
}
