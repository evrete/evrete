package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.evaluation.DefaultEvaluatorHandle;

import java.util.*;
import java.util.function.Consumer;

/**
 * Like every knowledge-level class in the library, this class converts rule builder into
 * a data structure that will be used by sessions to allocate Rete-specific memory elements.
 */
public final class KnowledgeRule extends AbstractActiveRule<KnowledgeFactGroup, KnowledgeLhs, AbstractRuntime<?, ?>> implements RuleDescriptor {

    private KnowledgeRule(AbstractRuntime<?, ?> runtime, DefaultRuleBuilder<?> other, int salience, KnowledgeLhs knowledgeLhs) {
        super(runtime, other, salience, knowledgeLhs);
    }

/*
    // Streams all the fact types with their memory information
    // We need this stream to initialize memory structures when this rule is deployed
    Stream<FactTypeDescriptor> typeDescriptorStream() {
        return Arrays
                .stream(lhsDescriptor.getFactGroups())
                .flatMap(group -> Arrays.stream(group.getEntryNodes()));
    }
*/

    static KnowledgeRule buildRule(AbstractRuntime<?, ?> runtime, DefaultRuleBuilder<?> rule, RuleBuilderActiveConditions lhsConditions, int salience) {
        // 1. Prepare fact declaration descriptors
        Collection<DefaultLhsBuilder.Fact> factDeclarations = rule.getLhs().rawValues();
        List<FactType> factTypes = new ArrayList<>(factDeclarations.size());

        for (DefaultLhsBuilder.Fact lhsFact : factDeclarations) {

            // Get alpha conditions for the given fact declaration
            Set<DefaultEvaluatorHandle> alphaHandles = lhsConditions.getAlphaConditionsOf(lhsFact.getVarName());

            // Build the runtime representation of the builder's fact declaration
            FactType factType = runtime.buildFactType(lhsFact, alphaHandles);

            // Add to the rule's LHS
            factTypes.add(factType);
        }

        // 2. Creating LHS descriptors.
        //    In this part, given the remaining unhandled beta-conditions and
        //    created fact type descriptors, we need to create the Rete evaluation nodes
        //    and, as a result, allocate fact type descriptors into groups.

        KnowledgeLhs knowledgeLhs = KnowledgeLhs.factory(factTypes, lhsConditions);

        return new KnowledgeRule(runtime, rule, salience, knowledgeLhs);
    }


    static <C extends RuntimeContext<C>> List<KnowledgeRule> buildRuleDescriptors(AbstractRuntime<?, C> runtime, DefaultRuleSetBuilder<C> ruleSetBuilder, Collection<RuleCompiledSources<DefaultRuleLiteralData, DefaultRuleBuilder<?>, DefaultConditionManager.Literal>> compiledSources) {
        // Finally we have all we need to create descriptor for each rule: compiled classes and original data in rule builders
        int currentRuleCount = runtime.getRules().size();
        List<DefaultRuleBuilder<C>> rules = ruleSetBuilder.getRuleBuilders();

        Map<DefaultRuleBuilder<?>, RuleCompiledSources<?, ?, DefaultConditionManager.Literal>> mapping = new IdentityHashMap<>();
        for (RuleCompiledSources<DefaultRuleLiteralData, ?, DefaultConditionManager.Literal> entry : compiledSources) {
            DefaultRuleBuilder<?> ruleBuilder = entry.getSources().getRule();
            mapping.put(ruleBuilder, entry);
        }

        List<KnowledgeRule> descriptors = new ArrayList<>(rules.size());
        ActiveEvaluatorGenerator evalCtx = runtime.getEvaluatorsContext();
        for (DefaultRuleBuilder<C> ruleBuilder : rules) {
            // 1. Check existing rules
            if (runtime.ruleExists(ruleBuilder.getName())) {
                throw new IllegalArgumentException("Rule '" + ruleBuilder.getName() + "' already exists");
            }

            // 2. Compute salience
            int salience = ruleBuilder.getSalience();
            if (salience == DefaultRuleBuilder.NULL_SALIENCE) {
                salience = -1 * (currentRuleCount + 1);
            }

            // 3. Register condition handles
            RuleBuilderActiveConditions ruleConditions = new RuleBuilderActiveConditions();

            DefaultConditionManager builderConditions = ruleBuilder.getConditionManager();


            // 3.2 Register functional conditions
            for (LhsConditionDH<String, ActiveField> evaluator : builderConditions.getEvaluators()) {
                // Add the condition
                ruleConditions.add(evaluator);
            }

            // 3.3 Register literal conditions
            Collection<DefaultConditionManager.Literal> literalConditions = builderConditions.getLiterals();
            if (!literalConditions.isEmpty()) {
                // There must be compiled copies for each literal condition
                RuleCompiledSources<?, ?, DefaultConditionManager.Literal> compiledData = mapping.get(ruleBuilder);

                if (compiledData == null) {
                    throw new IllegalStateException("No compiled data for literal sources");
                }

                // Create mapping
                Map<LiteralPredicate, CompiledPredicate<DefaultConditionManager.Literal>> conditionMap = new IdentityHashMap<>();
                for (CompiledPredicate<DefaultConditionManager.Literal> evaluator : compiledData.conditions()) {
                    conditionMap.put(evaluator.getSource(), evaluator);
                }

                // Register condition handles
                for (LiteralPredicate meta : literalConditions) {
                    CompiledPredicate<DefaultConditionManager.Literal> compiled = conditionMap.get(meta);
                    if (compiled == null) {
                        throw new IllegalStateException("Compiled condition not found for " + meta);
                    } else {
                        // 1. Registering the compiled predicate
                        LhsField.Array<String, ActiveField> descriptor = runtime.toActiveFields(compiled.resolvedFields());
                        DefaultEvaluatorHandle handle = evalCtx.addEvaluator(compiled.getPredicate(), compiled.getSource().getComplexity(), descriptor);
                        // 2. Complete the future
                        compiled.getSource().getHandle().complete(handle);
                        // 3. Now we have everything to know about the literal condition
                        ruleConditions.add(new LhsConditionDH<>(handle, descriptor));
                    }
                }
            }

            // 4. Handle literal RHS
            String literalRhs = ruleBuilder.getLiteralRhs();
            if (literalRhs != null) {
                RuleCompiledSources<?, ?, DefaultConditionManager.Literal> compiledData = mapping.get(ruleBuilder);

                if (compiledData == null) {
                    throw new IllegalStateException("No compiled data for literal sources");
                } else {
                    Consumer<RhsContext> compiledRhs = compiledData.rhs();
                    if (compiledRhs == null) {
                        throw new IllegalStateException("No compiled RHS for literal actions");
                    } else {
                        // Assign RHS action
                        ruleBuilder.setRhs(compiledRhs);
                    }
                }
            }


            // Build the descriptor and append it to the result
            KnowledgeRule descriptor = buildRule(runtime, ruleBuilder, ruleConditions, salience);
            descriptors.add(descriptor);

            currentRuleCount++;
        }
        return descriptors;
    }

    @Override
    public KnowledgeRule set(String property, Object value) {
        super.set(property, value);
        return this;
    }


}
