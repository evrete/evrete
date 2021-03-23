package org.evrete.api;

public interface RuntimeRule extends Rule {

    RuntimeRule addImport(RuleScope scope, String imp);

    KnowledgeSession<?> getRuntime();
}
