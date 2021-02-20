package org.evrete.runtime;

import org.evrete.api.KeyMode;
import org.evrete.api.ReIterator;
import org.evrete.api.RhsContext;
import org.evrete.api.ValueRow;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class RuntimeLhsDefault extends RuntimeLhs {
    RuntimeLhsDefault(RuntimeRuleImpl rule, LhsDescriptor descriptor) {
        super(rule, descriptor);
    }

    private static void runKeys(ScanMode mode, RhsFactGroupBeta[] groups, Runnable r) {
        switch (mode) {
            case DELTA:
                runDelta(0, groups.length - 1, false, groups, r);
                return;
            case KNOWN:
                runKnown(0, groups.length - 1, groups, r);
                return;
            case FULL:
                runFull(0, groups.length - 1, groups, r);
                return;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private static void runDelta(int index, int lastIndex, boolean hasDelta, RhsFactGroupBeta[] groups, Runnable r) {
        RhsFactGroupBeta group = groups[index];
        Set<Map.Entry<KeyMode, ReIterator<ValueRow[]>>> entries = group.keyIterators().entrySet();
        KeyMode mode;
        ReIterator<ValueRow[]> iterator;

        if (index == lastIndex) {
            for (Map.Entry<KeyMode, ReIterator<ValueRow[]>> entry : entries) {
                mode = entry.getKey();
                iterator = entry.getValue();
                if ((mode.isDeltaMode() || hasDelta) && iterator.reset() > 0) {
                    while (iterator.hasNext()) {
                        group.setKey(iterator.next());
                        r.run();
                    }
                }
            }

        } else {
            for (Map.Entry<KeyMode, ReIterator<ValueRow[]>> entry : entries) {
                mode = entry.getKey();
                iterator = entry.getValue();
                if (iterator.reset() > 0) {
                    while (iterator.hasNext()) {
                        group.setKey(iterator.next());
                        runDelta(index + 1, lastIndex, mode.isDeltaMode(), groups, r);
                    }
                }
            }
        }
    }

    private static void runFull(int index, int lastIndex, RhsFactGroupBeta[] groups, Runnable r) {
        RhsFactGroupBeta group = groups[index];
        Set<Map.Entry<KeyMode, ReIterator<ValueRow[]>>> entries = group.keyIterators().entrySet();
        ReIterator<ValueRow[]> iterator;

        if (index == lastIndex) {
            for (Map.Entry<KeyMode, ReIterator<ValueRow[]>> entry : entries) {
                iterator = entry.getValue();
                if (iterator.reset() > 0) {
                    while (iterator.hasNext()) {
                        group.setKey(iterator.next());
                        r.run();
                    }
                }
            }

        } else {
            for (Map.Entry<KeyMode, ReIterator<ValueRow[]>> entry : entries) {
                iterator = entry.getValue();
                if (iterator.reset() > 0) {
                    while (iterator.hasNext()) {
                        group.setKey(iterator.next());
                        runFull(index + 1, lastIndex, groups, r);
                    }
                }
            }
        }
    }

    private static void runKnown(int index, int lastIndex, RhsFactGroupBeta[] groups, Runnable r) {
        RhsFactGroupBeta group = groups[index];
        Set<Map.Entry<KeyMode, ReIterator<ValueRow[]>>> entries = group.keyIterators().entrySet();
        KeyMode mode;
        ReIterator<ValueRow[]> iterator;

        if (index == lastIndex) {
            for (Map.Entry<KeyMode, ReIterator<ValueRow[]>> entry : entries) {
                mode = entry.getKey();
                iterator = entry.getValue();
                //TODO !!!! optimize it, there's only one non-delta iterator!!!
                if ((!mode.isDeltaMode()) && iterator.reset() > 0) {
                    while (iterator.hasNext()) {
                        group.setKey(iterator.next());
                        r.run();
                    }
                }
            }

        } else {
            for (Map.Entry<KeyMode, ReIterator<ValueRow[]>> entry : entries) {
                mode = entry.getKey();
                iterator = entry.getValue();
                //TODO !!!! optimize it, there's only one non-delta iterator!!!
                if (iterator.reset() > 0 && (!mode.isDeltaMode())) {
                    while (iterator.hasNext()) {
                        group.setKey(iterator.next());
                        runKnown(index + 1, lastIndex, groups, r);
                    }
                }
            }
        }
    }

    @Override
    protected void forEach(Consumer<RhsContext> rhs) {
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
                    runKeys(
                            ScanMode.KNOWN,
                            betaGroups,
                            () -> RhsFactGroupBeta.runCurrentFacts(
                                    betaGroups,
                                    () -> alphaGroup.run(ScanMode.DELTA, eachFactRunnable)
                            )
                    );
                    runKeys(
                            ScanMode.DELTA,
                            betaGroups,
                            () -> RhsFactGroupBeta.runCurrentFacts(
                                    betaGroups,
                                    () -> alphaGroup.run(ScanMode.KNOWN, eachFactRunnable)
                            )
                    );
                    runKeys(
                            ScanMode.DELTA,
                            betaGroups,
                            () -> RhsFactGroupBeta.runCurrentFacts(
                                    betaGroups,
                                    () -> alphaGroup.run(ScanMode.DELTA, eachFactRunnable)
                            )
                    );
                } else {
                    //System.out.println("\t\t------- option 2");
                    runKeys(
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
                runKeys(
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
