package org.evrete.runtime.async;

import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.MemoryKeyCollection;
import org.evrete.api.ReIterator;
import org.evrete.runtime.*;
import org.evrete.util.Constants;

import java.util.Collection;
import java.util.LinkedList;

public class ConditionMemoryPurgeTask extends Completer {
    private static final long serialVersionUID = 7911593735991639599L;
    private final Collection<SubTask> subtasks = new LinkedList<>();

    public ConditionMemoryPurgeTask(Iterable<RuntimeRuleImpl> rules, Mask<MemoryAddress> keyPurgeMask) {
        for (RuntimeRuleImpl rule : rules) {
            for (BetaConditionNode node : rule.getLhs().getEndNodes()) {
                BetaConditionNode.forEachConditionNode(node, cn -> {
                    if (cn.hasMainStorage() && cn.getDescriptor().getMemoryMask().intersects(keyPurgeMask)) {
                        subtasks.add(new SubTask(ConditionMemoryPurgeTask.this, cn, keyPurgeMask));
                    }
                });
            }
        }
    }

    @Override
    protected void execute() {
        tailCall(subtasks, o -> o);
    }

    static class SubTask extends Completer {
        private static final long serialVersionUID = 8912306547512886112L;
        private final transient BetaConditionNode node;
        private final boolean[] checkFlags;
        private final MemoryKey[] buffer;

        SubTask(Completer completer, BetaConditionNode node, Mask<MemoryAddress> keyPurgeMask) {
            super(completer);
            this.node = node;

            FactType[] types = node.getDescriptor().getTypes();
            // Which types do we check
            this.checkFlags = new boolean[types.length];
            for (int i = 0; i < types.length; i++) {
                checkFlags[i] = types[i].getMemoryMask().intersects(keyPurgeMask);
            }
            this.buffer = new MemoryKey[types.length];

        }

        @Override
        protected void execute() {
            MemoryKeyCollection main = node.getStore(KeyMode.OLD_OLD);
            MemoryKeyCollection tempStore = node.getTempCollection();

            ReIterator<MemoryKey> it = main.iterator();
            if (it.reset() == 0) return;

            int i = 0;
            tempStore.clear();
            while (it.hasNext()) {
                MemoryKey key = it.next();
                buffer[i++] = key;
                if (i == buffer.length) {
                    //Reset buffer position
                    i = 0;
                    // Test the buffer values
                    if (test()) {
                        // Flush the buffer if data is valid
                        for (MemoryKey k : buffer) {
                            tempStore.add(k);
                        }
                    }
                }
            }
            // Copying the filtered data to the main storage
            main.clear();
            tempStore.forEach(main::add);
        }

        private boolean test() {
            for (int i = 0; i < buffer.length; i++) {
                if (checkFlags[i]) {
                    MemoryKey key = buffer[i];
                    if (key.getMetaValue() == Constants.DELETED_MEMORY_KEY_FLAG) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
