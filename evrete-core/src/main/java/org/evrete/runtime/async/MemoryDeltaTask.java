package org.evrete.runtime.async;

import org.evrete.runtime.TypeMemory;

import java.util.Iterator;

public class MemoryDeltaTask extends Completer {
    private static final long serialVersionUID = 7911593735990639599L;
    private final Iterator<TypeMemory> typeMemories;

    public MemoryDeltaTask(Iterator<TypeMemory> typeMemories) {
        this.typeMemories = typeMemories;
    }

    @Override
    protected void execute() {
        while (typeMemories.hasNext()) {
            TypeMemory item = typeMemories.next();

            Completer c = new TypeMemoryDeltaTask(this, item);
            addToPendingCount(1);
            if (typeMemories.hasNext()) {
                c.fork();
            } else {
                // Execute the tail in current thread
                c.compute();
            }
        }
    }


    static class TypeMemoryDeltaTask extends Completer {
        private static final long serialVersionUID = 7844452444442224060L;
        private final transient TypeMemory tm;

        TypeMemoryDeltaTask(Completer completer, TypeMemory tm) {
            super(completer);
            this.tm = tm;
        }

        @Override
        protected void execute() {
            tm.processBuffer();
        }
    }
}
