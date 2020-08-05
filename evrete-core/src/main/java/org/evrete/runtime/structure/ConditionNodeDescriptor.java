package org.evrete.runtime.structure;

import org.evrete.util.Bits;
import org.evrete.util.CollectionUtils;
import org.evrete.util.NextIntSupplier;

import java.util.*;

public class ConditionNodeDescriptor extends NodeDescriptor {
    public static final ConditionNodeDescriptor[] ZERO_ARRAY = new ConditionNodeDescriptor[0];
    private final EvaluatorGroup expression;
    private final int[] nonPlainSourceIndices;
    private final Map<Integer, TypeLocator> typeLocators = new HashMap<>();

    ConditionNodeDescriptor(NextIntSupplier idSupplier, EvaluatorGroup expression, Set<NodeDescriptor> sourceNodes) {
        super(idSupplier, sourceNodes);
        this.expression = expression;

        // Set data grouping for each source node
        List<Integer> nonPlainSourceIndicesList = new ArrayList<>();
        for (NodeDescriptor src : sourceNodes) {
            Set<FactType> allSourceTypes = new HashSet<>(Arrays.asList(src.getTypes()));

            Set<FactType> conditionTypes = new HashSet<>();
            Set<FactType> descriptor = expression.descriptor();
            for (FactType refType : descriptor) {
                if (allSourceTypes.contains(refType)) {
                    allSourceTypes.remove(refType);
                    conditionTypes.add(refType);
                }
            }

            FactType[] primary = FactType.toArray(conditionTypes);

            FactType[][] conditionGrouping;
            if (allSourceTypes.isEmpty()) {
                conditionGrouping = new FactType[1][];
                conditionGrouping[0] = primary;
            } else {
                FactType[] secondary = FactType.toArray(allSourceTypes);
                conditionGrouping = new FactType[2][];
                conditionGrouping[0] = primary;
                conditionGrouping[1] = secondary;
                nonPlainSourceIndicesList.add(src.getSourceIndex());
            }

            src.setEvalGrouping(conditionGrouping);
        }
        this.nonPlainSourceIndices = CollectionUtils.toIntArray(nonPlainSourceIndicesList, i -> i);

        // Create inverse location data structure

        for (FactType type : getTypes()) {
            TypeLocator locator = null;
            for (NodeDescriptor source : getSources()) {
                TypeLocator tl = TypeLocator.find(type, source);
                if (tl != null) {
                    if (locator == null) {
                        locator = tl;
                    } else {
                        throw new IllegalStateException("Integrity violation");
                    }
                }
            }

            if (locator == null) {
                throw new IllegalStateException("Integrity violation: " + type + " at " + this);
            } else {
                typeLocators.put(type.getInRuleIndex(), locator);
            }
        }
    }


    static Collection<ConditionNodeDescriptor> allocateConditions(Collection<FactType> betaTypes, List<EvaluatorGroup> list) {
        final Set<NodeDescriptor> unallocatedNodes = new HashSet<>();
        NextIntSupplier idSupplier = new NextIntSupplier();
        for (FactType factType : betaTypes) {
            EntryNodeDescriptor e = new EntryNodeDescriptor(idSupplier, factType);
            unallocatedNodes.add(e);
        }

        EvaluatorGroup[] evaluatorSequence = list.toArray(EvaluatorGroup.ZERO_ARRAY);

        // Loop through the expressions one by one
        // The initial order of expressions defines the outcome.
        for (EvaluatorGroup evaluator : evaluatorSequence) {
            Set<NodeDescriptor> matching = Bits.matchesOR(evaluator.getTypeMask(), unallocatedNodes, NodeDescriptor::getMask);
            assert !matching.isEmpty();
            // replace the matching nodes with a new one
            unallocatedNodes.removeAll(matching);
            unallocatedNodes.add(new ConditionNodeDescriptor(idSupplier, evaluator, matching));
        }
        Collection<ConditionNodeDescriptor> finalNodes = new ArrayList<>(unallocatedNodes.size());
        for (NodeDescriptor nd : unallocatedNodes) {
            if (nd.isConditionNode()) {
                ConditionNodeDescriptor cnd = (ConditionNodeDescriptor) nd;
                cnd.setEvalGrouping(new FactType[][]{cnd.getTypes()});
                finalNodes.add(cnd);
            }
        }
        return finalNodes;
    }

    @Override
    public boolean isConditionNode() {
        return true;
    }

    public TypeLocator locate(FactType type) {
        TypeLocator locator = typeLocators.get(type.getInRuleIndex());
        if (locator == null) {
            throw new IllegalArgumentException();
        } else {
            return locator;
        }
    }

    public int[] getNonPlainSourceIndices() {
        return nonPlainSourceIndices;
    }

    public EvaluatorGroup getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return "{id=" + getIndex() +
                ", expression=" + expression +
                ", mask=" + getMask() +
                '}';
    }

    public static class TypeLocator {
        public final int source;
        public final int level;
        public final int position;

        private TypeLocator(int source, int level, int position) {
            this.source = source;
            this.level = level;
            this.position = position;
        }

        static TypeLocator find(FactType subject, NodeDescriptor source) {
            FactType[][] grouping = source.getEvalGrouping();
            for (int level = 0; level < grouping.length; level++) {
                FactType[] levelTypes = grouping[level];
                for (int position = 0; position < levelTypes.length; position++) {
                    FactType type = levelTypes[position];
                    if (type.getInRuleIndex() == subject.getInRuleIndex()) {
                        return new TypeLocator(source.getSourceIndex(), level, position);
                    }
                }
            }
            return null;

        }
    }
}
