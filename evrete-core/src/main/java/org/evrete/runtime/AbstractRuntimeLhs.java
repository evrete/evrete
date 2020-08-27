package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.memory.BetaEndNode;

import java.util.*;
import java.util.function.BooleanSupplier;


public abstract class AbstractRuntimeLhs {
    private final Collection<BetaEndNode> endNodes = new ArrayList<>();
    private final AbstractLhsDescriptor descriptor;
    private final AbstractRuntimeLhs parent;
    private final Map<ConditionNodeDescriptor, BetaEndNode> betaEndNodeMap = new HashMap<>();

    final RuntimeFact[][] factState;
    private final ValueRow[][] keyState;
    final boolean hasBetaNodes;
    private final NestedFactRunnable[] factGroupIterators;
    private final RhsKeysGroupIterator[] keyIterators;
    private final RhsFactGroupIterator looseFactGroupIterator;
    private final NestedFactRunnable factIterator;
    private final NestedFactRunnable lastFactGroupIterator;
    private final boolean fullKeyScan;
    //private final Runnable keyIterator;
    //private final RhsKeysGroupIterator lastKeyIterator;

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

            //RhsFactGroupIterator groupIterator = new RhsFactGroupIterator(factGroupId, factState);
            RhsFactGroupIterator groupIterator;
            if (groupDescriptor.isLooseGroup()) {
                // For a loose (alpha) group we're able to init fact iterators immediately
                // because they won't ever change
                groupIterator = new RhsFactGroupIteratorLoose(factGroupId, rule.resolve(RuntimeFactTypePlain.class, types), factState);
                if (looseIterator == null) {
                    looseIterator = groupIterator;
                } else {
                    // Duplicate loose iterator
                    throw new IllegalStateException();
                }
            } else {
                ConditionNodeDescriptor finalNode = groupDescriptor.getFinalNode();
                RhsKeysGroupIterator iterator;
                int keyGroupId = groupDescriptor.getKeyGroupIndex();
                if (finalNode != null) {
                    BetaEndNode endNode = betaEndNodeMap.get(finalNode);
                    groupIterator = new RhsFactGroupIteratorKeyed(factGroupId, factState);
                    iterator = new RhsKeysGroupIterator(keyGroupId, endNode, groupIterator, keyState);
                } else {
                    throw new UnsupportedOperationException();
/*
                    assert types.length == 1;
                    RuntimeFactTypeKeyed runtimeFactType = rule.resolve(types[0]);
                    rtFactTypes = new RuntimeFactTypeKeyed[]{rule.resolve(types[0])};
                    mainIterator = runtimeFactType.mainIterator();
                    deltaIterator = runtimeFactType.deltaIterator();
*/
                }



                // Save and chain key iterators
                keyIterators[keyGroupId] = iterator;
/*
                if (keyGroupId > 0) {
                    RhsKeysGroupIterator prev = keyIterators[keyGroupId - 1];
                    prev.setNested(iterator);
                }
*/
            }

            //Save and chain the FactGroupIterator
            factGroupIterators[factGroupId] = groupIterator;
            if (i > 0) {
                NestedFactRunnable prev = factGroupIterators[i - 1];
                prev.setDelegate(groupIterator);
            }
        }

        this.looseFactGroupIterator = looseIterator;
        this.fullKeyScan = looseIterator != null;
        this.factIterator = factGroupIterators[0];
        this.lastFactGroupIterator = factGroupIterators[factGroupIterators.length - 1];
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

    public AbstractLhsDescriptor getDescriptor() {
        return descriptor;
    }

    /*
    void forEachKey(Runnable r) {
        lastKeyIterator.setRunnable(r);
        keyIterator.run();
    }
*/

    void forEachKey(Runnable r) {
        forEachKey(0, false, r);
    }

    void forEachKey(int index, boolean hasDelta, Runnable r) {
        RhsKeysGroupIterator groupIterator = keyIterators[index];
        EnumMap<KeyMode, ReIterator<ValueRow[]>> modeIterators = groupIterator.keyIterators();
        KeyMode mode;
        boolean iteratorPass;
        ReIterator<ValueRow[]> i;

        if(index == keyIterators.length - 1) {
            // The last key
            for(Map.Entry<KeyMode, ReIterator<ValueRow[]>> entry : modeIterators.entrySet()) {
                mode = entry.getKey();
                i = entry.getValue();
                iteratorPass = fullKeyScan || hasDelta || mode.isDeltaMode();

                if(iteratorPass && i.reset() > 0) {
                    while (i.hasNext()) {
                        groupIterator.initFactIterators(i.next());
                        r.run();
                    }
                }
            }
        } else {
            // A middle key
            for(Map.Entry<KeyMode, ReIterator<ValueRow[]>> entry : modeIterators.entrySet()) {
                mode = entry.getKey();
                i = entry.getValue();
                if(i.reset() > 0) {
                    while (i.hasNext()) {
                        groupIterator.initFactIterators(i.next());
                        forEachKey(index + 1, mode.isDeltaMode(), r);
                    }
                }
            }
        }
    }

    void forEachFact(NestedFactRunnable r) {
        lastFactGroupIterator.setDelegate(r);
        factIterator.forEachFact();
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
