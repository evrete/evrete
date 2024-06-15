package org.evrete.runtime;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

class RuntimeRules extends SearchList<SessionRule> {

    void forEachFactGroup(Consumer<SessionFactGroup> consumer) {
        for (SessionRule sessionRule : this) {
            for(SessionFactGroup group : sessionRule.getLhs().getFactGroups()) {
                consumer.accept(group);
            }
        }
    }

    Stream<SessionFactGroup> getFactGroups() {
        return this.stream().flatMap(sessionRule -> Arrays.stream(sessionRule.getLhs().getFactGroups()));
    }

    Stream<SessionFactGroupBeta> getBetaFactGroups() {
        return getFactGroups().filter(g->!g.isPlain()).map(group -> (SessionFactGroupBeta) group);
    }
}
