package org.evrete.runtime.async;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CountedCompleter;
import java.util.function.Function;

public abstract class Completer extends CountedCompleter<Void> {
    private static final long serialVersionUID = -1753467515874328504L;
    private boolean directInvoke = false;

    Completer(Completer completer) {
        super(completer);
        this.directInvoke = false;
    }

    Completer() {
        super();
    }


    public static Completer of(Collection<? extends Runnable> collection) {
        switch (collection.size()) {
            case 0:
                throw new IllegalArgumentException();
            case 1:
                return new RunnableCompleter(null, collection.iterator().next());
            default:
                return new MultiRunnableCompleter(collection);
        }
    }

    protected abstract void execute();

    boolean isDirectInvoke() {
        return directInvoke;
    }

    private void invokeDirect() {
        directInvoke = true;
        execute();
        onCompletion(this);
    }

    @Override
    public final void compute() {
        this.execute();
        tryComplete();
    }

    void forkNew(Completer completer) {
        if (directInvoke) {
            completer.invokeDirect();
        } else {
            addToPendingCount(1);
            completer.fork();
        }
    }

    <Z> void tailCall(Collection<Z> collection, Function<Z, Completer> mapper) {
        if (directInvoke) {
            for (Z o : collection) {
                mapper.apply(o).invokeDirect();
            }
        } else {
            Iterator<Z> it = collection.iterator();
            while (it.hasNext()) {
                Z item = it.next();
                Completer c = mapper.apply(item);
                addToPendingCount(1);
                if (it.hasNext()) {
                    c.fork();
                } else {
                    // Execute the tail in current thread
                    c.compute();
                }
            }
        }
    }

    private static class MultiRunnableCompleter extends Completer {
        private static final long serialVersionUID = -243409304205835246L;
        private final Collection<? extends Runnable> collection;

        MultiRunnableCompleter(Collection<? extends Runnable> collection) {
            super(null);
            assert collection.size() > 1;
            this.collection = collection;
        }

        @Override
        protected void execute() {
            if (isDirectInvoke()) {
                for (Runnable r : collection) {
                    r.run();
                }
            } else {
                Iterator<? extends Runnable> it = collection.iterator();
                Runnable first = it.next();
                while (it.hasNext()) {
                    forkNew(new RunnableCompleter(this, it.next()));
                }
                first.run();
            }
        }
    }

    static class RunnableCompleter extends Completer {
        private static final long serialVersionUID = -3448763603811865456L;
        private final Runnable runnable;

        RunnableCompleter(Completer completer, Runnable runnable) {
            super(completer);
            this.runnable = runnable;
        }

        @Override
        protected void execute() {
            runnable.run();
        }
    }
}
