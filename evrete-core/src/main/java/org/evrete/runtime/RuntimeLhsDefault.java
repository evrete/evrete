package org.evrete.runtime;

import org.evrete.api.RhsContext;
import org.evrete.runtime.memory.Buffer;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class RuntimeLhsDefault extends RuntimeLhs implements RhsContext, MemoryChangeListener {
    private static final Collection<RuntimeAggregateLhsJoined> EMPTY_AGGREGATES = Collections.unmodifiableCollection(Collections.emptyList());

    RuntimeLhsDefault(RuntimeRuleImpl rule, LhsDescriptor descriptor, Buffer buffer) {
        super(rule, descriptor, buffer);
    }

    @Override
    public Collection<RuntimeAggregateLhsJoined> getAggregateConditionedGroups() {
        return EMPTY_AGGREGATES;
    }

    @Override
    public void forEach(Consumer<RhsContext> rhs) {
        forEach(() -> rhs.accept(this));
    }

    private void forEach(Runnable eachFactRunnable) {
        RhsFactGroupAlpha alphaGroup = getAlphaFactGroup();
        RhsFactGroupBeta[] betaGroups = getBetaFactGroups();
        if (alphaGroup != null) {
            boolean hasAlphaDelta = alphaGroup.hasDelta();
            if (betaGroups.length > 0) {
                // Alpha-Beta
                //System.out.println("------- Alpha-Beta");
                if (hasAlphaDelta) {
                    RhsFactGroupBeta.runKeys(
                            ScanMode.KNOWN,
                            betaGroups,
                            () -> RhsFactGroupBeta.runCurrentFacts(
                                    betaGroups,
                                    () -> alphaGroup.run(ScanMode.DELTA, eachFactRunnable)
                            )
                    );
                    RhsFactGroupBeta.runKeys(
                            ScanMode.DELTA,
                            betaGroups,
                            () -> RhsFactGroupBeta.runCurrentFacts(
                                    betaGroups,
                                    () -> alphaGroup.run(ScanMode.KNOWN, eachFactRunnable)
                            )
                    );
                    RhsFactGroupBeta.runKeys(
                            ScanMode.DELTA,
                            betaGroups,
                            () -> RhsFactGroupBeta.runCurrentFacts(
                                    betaGroups,
                                    () -> alphaGroup.run(ScanMode.DELTA, eachFactRunnable)
                            )
                    );
                } else {
                    //System.out.println("\t\t------- option 2");
                    RhsFactGroupBeta.runKeys(
                            ScanMode.DELTA,
                            betaGroups,
                            () -> {
                                //System.out.println("\t" + i1.incrementAndGet());
                                RhsFactGroupBeta.runCurrentFacts(
                                        betaGroups,
                                        () -> {
                                            //System.out.println("\t\t" + i2.incrementAndGet());
                                            alphaGroup.run(ScanMode.FULL, eachFactRunnable);
                                        }
                                );
                            }
                    );
                }
            } else {
                // Alpha-NoBeta
                //System.out.println("------- Alpha-NoBeta");
                alphaGroup.run(ScanMode.DELTA, eachFactRunnable);
            }
        } else {
            if (betaGroups.length > 0) {
                // NoAlpha/Beta
                //System.out.println("------- NoAlpha-Beta");
                RhsFactGroupBeta.runKeys(
                        ScanMode.DELTA,
                        betaGroups,
                        () -> RhsFactGroupBeta.runCurrentFacts(betaGroups, eachFactRunnable)
                );
            } else {
                // NoAlpha/NoBeta
                Logger.getAnonymousLogger().warning("No output groups");
            }
        }
    }

}
