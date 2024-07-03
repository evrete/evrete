package org.evrete.runtime;

import org.evrete.api.Rule;
import org.evrete.api.RuleSession;
import org.evrete.api.RuntimeRule;
import org.evrete.api.annotations.Nullable;
import org.evrete.util.CommonUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * Base session class with all the methods related to rule deployments
 * </p>
 *
 * @param <S> session type parameter
 */
public abstract class AbstractRuleSessionDeployment<S extends RuleSession<S>> extends AbstractRuleSessionOps<S> {
    final RuntimeRules ruleStorage;

    AbstractRuleSessionDeployment(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.ruleStorage = new RuntimeRules();
    }

    void deployRules(List<KnowledgeRule> descriptors, boolean hotDeployment) {
        // 1. Allocate required alpha memory elements
        CompletableFuture<Void> memoryAllocation = malloc(descriptors);

        // 2. Turn knowledge rules into their session versions
        CompletableFuture<List<SessionRule>> convertedRules = memoryAllocation
                .thenCompose(v -> CommonUtils.completeAndCollect(descriptors, this::deploySingleRule));

        // 3. Depending on the mode, align the rules' beta memories with existing data
        CompletableFuture<List<SessionRule>> deployedRules = convertedRules
                .thenCompose(rules -> allocateBetaNodes(rules, hotDeployment));

        // 4. Finally, sort and append the rules
        CompletableFuture<Void> finish = deployedRules
                .thenAccept(rules -> ruleStorage.addAllAndSort(rules, getRuleComparator()));

        // 5. Wait for the completion
        finish.join();
    }

    private CompletableFuture<List<SessionRule>> allocateBetaNodes(List<SessionRule> sessionRules, boolean hotDeployment) {
        if (hotDeployment) {
            List<CompletableFuture<Void>> betaUpdateTasks = new ArrayList<>(sessionRules.size());
            for (SessionRule r : sessionRules) {
                betaUpdateTasks.add(r.getLhs().buildDeltas(DeltaMemoryMode.HOT_DEPLOYMENT));
            }
            return CommonUtils.completeAll(betaUpdateTasks).thenApply(ignored -> sessionRules);
        } else {
            return CompletableFuture.completedFuture(sessionRules);
        }
    }

    private CompletableFuture<Void> malloc(Collection<KnowledgeRule> rules) {
        MapOfSet<ActiveType.Idx, AlphaAddress> alphaMemoriesByType = new MapOfSet<>();
        for (KnowledgeRule rule : rules) {
            for (KnowledgeFactGroup group : rule.getLhs().getFactGroups()) {
                for (FactType factType : group.getEntryNodes()) {
                    alphaMemoriesByType.add(factType.type().getId(), factType.getAlphaAddress());
                }
            }
        }
        return CommonUtils.completeAll(
                alphaMemoriesByType.entrySet(),
                entry -> getMemory().allocateMemoryIfNotExists(entry.getKey(), entry.getValue())
        );
    }

    private CompletableFuture<SessionRule> deploySingleRule(KnowledgeRule rule) {
        return CompletableFuture.supplyAsync(
                () -> new SessionRule(rule, AbstractRuleSessionDeployment.this),
                getService().getExecutor()
        );
    }


    private void reSortRules() {
        ruleStorage.sort(getRuleComparator());
    }


    @Override
    void addRuleDescriptors(List<KnowledgeRule> newRules) {
        deployRules(newRules, true);
    }

    @Override
    public void setRuleComparator(Comparator<Rule> ruleComparator) {
        super.setRuleComparator(ruleComparator);
        reSortRules();
    }

    @Override
    @Nullable
    public final RuntimeRule getRule(String name) {
        return ruleStorage.get(name);
    }

    @Override
    public List<RuntimeRule> getRules() {
        return Collections.unmodifiableList(ruleStorage.getList());
    }

}
