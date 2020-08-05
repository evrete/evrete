package org.evrete;

import org.evrete.api.Knowledge;
import org.evrete.runtime.KnowledgeImpl;
import org.evrete.runtime.async.ForkJoinExecutor;

public class KnowledgeService {
    private final Configuration configuration;
    private final ForkJoinExecutor executor = new ForkJoinExecutor();

    public KnowledgeService(Configuration configuration) {
        this.configuration = configuration;
    }

    public KnowledgeService() {
        this(new Configuration());
    }

    public Knowledge newKnowledge() {
        return new KnowledgeImpl(configuration, executor);
    }

    public void shutdown() {
        this.executor.shutdown();
    }
}
