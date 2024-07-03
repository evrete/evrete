package org.evrete.runtime;

import org.evrete.util.CommonUtils;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class SessionLhs extends ActiveLhs<SessionFactGroup> {

    private SessionLhs(KnowledgeLhs descriptor, Function<KnowledgeFactGroup, SessionFactGroup> mapper) {
        super(SessionFactGroup.class, descriptor, mapper);
    }

    CompletableFuture<Void> buildDeltas(DeltaMemoryMode mode) {
        return CommonUtils.completeAll(getFactGroups(), g -> g.buildDeltas(mode));
    }

    static SessionLhs factory(AbstractRuleSessionBase<?> runtime, KnowledgeLhs descriptor) {
        return new SessionLhs(descriptor, group -> SessionFactGroup.factory(runtime, group));
    }

    @Override
    public String toString() {
        return "{" +
                "groups=" + Arrays.toString(getFactGroups()) +
                '}';
    }
}
