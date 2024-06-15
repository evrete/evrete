package org.evrete.runtime;

import org.evrete.util.CommonUtils;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Logger;

public class SessionLhs extends ActiveLhs<SessionFactGroup> {
    private static final Logger LOGGER = Logger.getLogger(SessionLhs.class.getName());

    int i = 0;

    private SessionLhs(KnowledgeLhs descriptor, Function<KnowledgeFactGroup, SessionFactGroup> mapper) {
        super(SessionFactGroup.class, descriptor, mapper);
    }

    CompletableFuture<Void> buildDeltas(DeltaMemoryMode mode) {
        return CommonUtils.completeAll(getFactGroups(), g -> g.buildDeltas(mode));
    }

    CompletableFuture<Void> commitDeltas() {
        return CommonUtils.completeAll(getFactGroups(), SessionFactGroup::commitDeltas);
    }

//    CompletableFuture<Void> processDeleteDeltaActions(int typeId, Set<FactFieldValues> valuesToDelete) {
//        return CommonUtils.completeAll(
//                getFactGroups(),
//                group -> group.processDeleteDeltaActions(typeId, valuesToDelete)
//        );
//    }

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
