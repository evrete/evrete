package org.evrete.runtime;

import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeRule;
import org.evrete.api.spi.MemoryScope;
import org.evrete.util.CombinationIterator;
import org.evrete.util.MapFunction;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class SessionRule extends AbstractActiveRule<SessionFactGroup, SessionLhs, AbstractRuleSessionOps<?>> implements RuntimeRule {
    //private final RhsContextImpl rhsContext;
    private static final Logger LOGGER = Logger.getLogger(SessionRule.class.getName());
    /**
     * This is a shared target array for the fact iterator. Instead of creating a new array for each
     * RHS iteration, this array contains the current facts grouped by fact groups.
     */
    private final DefaultFactHandle[][] currentGroupedFacts;
    private final MapFunction<String, KnowledgeLhs.FactPosition> factPositionMapping;

    SessionRule(KnowledgeRule knowledgeRule, AbstractRuleSessionOps<?> sessionRuntime) {
        super(sessionRuntime, knowledgeRule, SessionLhs.factory(sessionRuntime, knowledgeRule.getLhs()));
        SessionFactGroup[] factGroups = getLhs().getFactGroups();

        this.currentGroupedFacts = new DefaultFactHandle[factGroups.length][];
        this.factPositionMapping = knowledgeRule.getLhs().getFactPositionMapping();

        LOGGER.fine(() -> "Session rule created: " + this);
    }


    final long callRhs(WorkMemoryActionBuffer destinationForRuleActions) {
        LOGGER.fine(() -> "RHS START for rule '" + this.getName() + "'");
        // Initializing RHS vars
        final Consumer<RhsContext> ruleRhs = getRhs();
        final RhsContextImpl rhsContext = new RhsContextImpl(this, this.currentGroupedFacts, this.factPositionMapping, getLhs().getFactGroups(), destinationForRuleActions);

        // Preparing the iterator
        SessionFactGroup[] groups = getLhs().getFactGroups();
        Iterator<MemoryScope[]> scopesIterator = MemoryScope.states(MemoryScope.DELTA, new MemoryScope[groups.length]);

        // Start the iteration
        scopesIterator.forEachRemaining(scopes -> callRhs(groups, scopes, ruleRhs, rhsContext));

        LOGGER.fine(() -> "RHS END for rule '" + this.getName() + "'");
        return rhsContext.activationCount.get();
    }

    private void callRhs(SessionFactGroup[] groups, MemoryScope[] scopes, Consumer<RhsContext> ruleRhs, RhsContextImpl rhsContext) {
        LOGGER.fine(() -> "RHS memory scopes for groups: " + Arrays.toString(scopes));

        // Given the scopes, create an iterator over fact handles inside the LHS fact groups
        Iterator<DefaultFactHandle[][]> joinedFacts = new CombinationIterator<>(
                currentGroupedFacts,
                index -> groups[index].factHandles(scopes[index])
        );

        // Iterate over fact handles and perform the rule's RHS action
        joinedFacts.forEachRemaining(ignored -> {
            // We're ignoring the iterator argument because it's a shared array
            ruleRhs.accept(rhsContext.next());
        });

    }

    MapFunction<String, KnowledgeLhs.FactPosition> getFactPositionMapping() {
        return factPositionMapping;
    }

    @Override
    public String toString() {
        return "{" +
                "name='" + getName() + "'" +
                ", lhs=" + getLhs() +
                '}';
    }

    public void clear() {
        //TODO review the usage
    }

    @Override
    public RuntimeRule set(String property, Object value) {
        super.set(property, value);
        return this;
    }

}
