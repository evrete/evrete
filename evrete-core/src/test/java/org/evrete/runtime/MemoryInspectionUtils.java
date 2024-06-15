package org.evrete.runtime;

import org.evrete.api.FactHandle;
import org.evrete.api.RuleSession;
import org.evrete.api.spi.MemoryScope;
import org.evrete.runtime.rete.*;
import org.evrete.util.MapFunction;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MemoryInspectionUtils {

    @SuppressWarnings("unchecked")
    private static <T extends RuleSession<T>> AbstractRuleSession<T> cast(RuleSession<?> session) {
        return (AbstractRuleSession<T>) session;
    }

    static SessionFactType factDeclaration(RuleSession<?> session, String ruleName, String factName) {
        AbstractRuleSession<?> s = cast(session);
        SessionRule rule = (SessionRule) s.getRule(ruleName);
        MapFunction<String, KnowledgeLhs.FactPosition> factSearchFunction = rule.getFactPositionMapping();
        KnowledgeLhs.FactPosition pos = factSearchFunction.apply(factName);
        return rule.getLhs().getFactGroups()[pos.groupIndex].getFactTypes()[pos.inGroupIndex];
    }

    static Stream<FactHolder> alphaMemoryContents(RuleSession<?> session, AlphaAddress address) {
        MemoryScope scope = MemoryScope.MAIN;
        AbstractRuleSession<?> s = cast(session);
        TypeAlphaMemory alphaMemory = s.getMemory().getAlphaMemory(address);
        TypeMemory typeMemory = s.getMemory().getTypeMemory(address.getType());
        return alphaMemory.stream(scope).flatMap(values -> alphaMemory.stream(scope, values).map(typeMemory::get));
    }

    static WorkMemoryActionBuffer.State bufferedContent(RuleSession<?> session, FactHandle handle) {
        DefaultFactHandle h = (DefaultFactHandle) handle;
        AbstractRuleSession<?> s = cast(session);
        return s.getActionBuffer().getState(h);
    }


    static void assertNoDeltaStates(RuleSession<?> session) {
        AbstractRuleSession<?> s = cast(session);

        SessionMemory memory = s.getMemory();

        // No deployments must be in progress
        assert memory.getTypeMemoryDeployments().taskCount() == 0;

        // Inspecting delta memories
        memory.getAlphaMemories().forEach(alphaMemory -> {
            assert !alphaMemory.keyIterator(MemoryScope.DELTA).hasNext() : "Alpha memory " + alphaMemory + " has delta state";
        });

        // Inspecting beta nodes of each rule
        for (SessionRule rule : s.ruleStorage) {
            forEachConditionNode(rule, node -> {
                assert node.isConditionNode();
                ConditionMemory betaMemory = node.getBetaMemory();
                assert !betaMemory.iterator(MemoryScope.DELTA).hasNext() : "Node " + node + " has delta state";
            });
        }
    }

    static Set<AlphaAddress> getAlphaConditions(RuleSession<?> session, Class<?> javaType) {
        AbstractRuleSession<?> s = cast(session);
        ActiveType activeType = s.activeTypes()
                .filter(type -> type.getValue().getJavaClass().equals(javaType))
                .findFirst().orElseThrow();
        return new HashSet<>(activeType.getKnownAlphaLocations());
    }

    private static void forEachConditionNode(SessionRule rule, Consumer<ReteSessionConditionNode> consumer) {
        SessionFactGroup[] groups = rule.getLhs().getFactGroups();
        for (SessionFactGroup group : groups) {
            if (!group.isPlain()) {
                SessionFactGroupBeta betaGroup = (SessionFactGroupBeta) group;
                ReteGraph<ReteSessionNode, ReteSessionEntryNode, ReteSessionConditionNode> graph = betaGroup.getGraph();
                graph.forEachConditionNode(consumer);
            }
        }

    }
}
