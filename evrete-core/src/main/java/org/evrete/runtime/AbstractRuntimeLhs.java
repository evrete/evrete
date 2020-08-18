package org.evrete.runtime;

import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.api.ValueRow;
import org.evrete.runtime.memory.BetaEndNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;


public abstract class AbstractRuntimeLhs {
    private final Collection<BetaEndNode> endNodes = new ArrayList<>();
    private final AbstractLhsDescriptor descriptor;
    private final AbstractRuntimeLhs parent;
    private final Map<ConditionNodeDescriptor, BetaEndNode> betaEndNodeMap = new HashMap<>();

    final RuntimeFact[][] factState;
    private final ValueRow[][] keyState;
    final boolean hasBetaNodes;
    private final RhsFactGroupIterator[] factGroupIterators;
    private final RhsKeysGroupIterator[] keyIterators;
    private final RhsFactGroupIterator looseFactGroupIterator;
    private final Runnable factIterator;
    private final RhsFactGroupIterator lastFactGroupIterator;
    private final Runnable keyIterator;
    private final RhsKeysGroupIterator lastKeyIterator;

    AbstractRuntimeLhs(RuntimeRuleImpl rule, AbstractRuntimeLhs parent, AbstractLhsDescriptor descriptor) {
        this.descriptor = descriptor;
        this.parent = parent;

        // Init end nodes first
        // (this should stay in this class, unlike keys and fact iterators)
        for (RhsFactGroupDescriptor groupDescriptor : descriptor.getAllFactGroups()) {
            ConditionNodeDescriptor finalNode = groupDescriptor.getFinalNode();
            if (finalNode != null) {
                BetaEndNode endNode = new BetaEndNode(rule, finalNode);
                endNodes.add(endNode);
                betaEndNodeMap.put(finalNode, endNode);
            }
        }


        // Init iteration states
        RhsFactGroupDescriptor[] allFactGroups = descriptor.getAllFactGroups();
        this.factGroupIterators = new RhsFactGroupIterator[allFactGroups.length];
        this.keyIterators = new RhsKeysGroupIterator[descriptor.getBetaFactGroupCount()];
        this.keyState = new ValueRow[descriptor.getBetaFactGroupCount()][];
        this.factState = new RuntimeFact[allFactGroups.length][];

        RhsFactGroupIterator looseIterator = null;
        for (int i = 0; i < allFactGroups.length; i++) {
            RhsFactGroupDescriptor groupDescriptor = allFactGroups[i];
            int factGroupId = groupDescriptor.getFactGroupIndex();
            assert factGroupId == i;
            FactType[] types = groupDescriptor.getTypes();
            this.factState[factGroupId] = new RuntimeFact[types.length];

            RhsFactGroupIterator groupIterator = new RhsFactGroupIterator(factGroupId, factState);
            if (groupDescriptor.isLooseGroup()) {
                // For a loose (alpha) group we're able to init fact iterators immediately
                // because they won't ever change
                groupIterator.setIterables(rule.resolve(types));
                if (looseIterator == null) {
                    looseIterator = groupIterator;
                } else {
                    throw new IllegalStateException();
                }
            } else {
                ReIterator<ValueRow[]> mainIterator;
                ReIterator<ValueRow[]> deltaIterator;
                ConditionNodeDescriptor finalNode = groupDescriptor.getFinalNode();
                if (finalNode != null) {
                    BetaEndNode endNode = resolve(finalNode);
                    mainIterator = endNode.mainIterator();
                    deltaIterator = endNode.deltaIterator();
                } else {
                    assert types.length == 1;
                    RuntimeFactTypeKeyed runtimeFactType = rule.resolve(types[0]);
                    mainIterator = runtimeFactType.mainIterator();
                    deltaIterator = runtimeFactType.deltaIterator();
                }

                int keyGroupId = groupDescriptor.getKeyGroupIndex();
                RhsKeysGroupIterator iterator = RhsKeysGroupIterator.factory(keyGroupId, groupDescriptor, groupIterator, mainIterator, deltaIterator, keyState);


                // Save and chain key iterators
                keyIterators[keyGroupId] = iterator;
                if (keyGroupId > 0) {
                    RhsKeysGroupIterator prev = keyIterators[keyGroupId - 1];
                    prev.setRunnable(iterator);
                }
            }

            //Save and chain the FactGroupIterator
            factGroupIterators[factGroupId] = groupIterator;
            if (i > 0) {
                RhsFactGroupIterator prev = factGroupIterators[i - 1];
                prev.setDelegate(groupIterator);
            }
        }

        this.looseFactGroupIterator = looseIterator;
        this.factIterator = factGroupIterators[0];
        this.lastFactGroupIterator = factGroupIterators[factGroupIterators.length - 1];


        // Init key iterators
        if (keyIterators.length > 0) {
            this.keyIterator = keyIterators[0];
            this.lastKeyIterator = keyIterators[keyIterators.length - 1];
        } else {
            this.keyIterator = null;
            this.lastKeyIterator = null;
        }

        this.hasBetaNodes = keyIterators.length > 0;

    }

    void addStateKeyPredicate(RhsFactGroupDescriptor descriptor, BooleanSupplier predicate) {
        this.keyIterators[descriptor.getKeyGroupIndex()].addStateKeyPredicate(predicate);
    }

    RhsKeysGroupIterator resolve(RhsFactGroupDescriptor descriptor) {
        return keyIterators[descriptor.getKeyGroupIndex()];
    }

    public ValueRow[][] getKeyState() {
        return keyState;
    }

    public RhsFactGroupIterator getLooseFactGroupIterator() {
        return looseFactGroupIterator;
    }

    AbstractRuntimeLhs(RuntimeRuleImpl rule, LhsDescriptor descriptor) {
        this(rule, null, descriptor);
    }

    public RhsKeysGroupIterator[] getKeyGroupIterators() {
        return keyIterators;
    }

    void forEachKey(Runnable r) {
        lastKeyIterator.setRunnable(r);
        keyIterator.run();
    }

    void forEachFact(Runnable r) {
        lastFactGroupIterator.setDelegate(r);
        factIterator.run();
    }

    public final BetaEndNode resolve(ConditionNodeDescriptor descriptor) {
        AbstractRuntimeLhs current = this;
        while (current != null) {
            BetaEndNode node = betaEndNodeMap.get(descriptor);
            if (node != null) {
                return node;
            }
            current = current.parent;
        }
        throw new IllegalArgumentException("Node not found or the argument is not an end-node");
    }

    Collection<BetaEndNode> getEndNodes() {
        return endNodes;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "descriptor=" + descriptor +
                '}';
    }
}
