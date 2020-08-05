package org.evrete.runtime.async;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CountedCompleter;
import java.util.function.Function;

public abstract class Completer extends CountedCompleter<Void> {
    private boolean directInvoke = false;

    public Completer(Completer completer) {
        super(completer);
    }

    public Completer() {
        super();
    }

    public static Completer of(Collection<? extends Runnable> collection) {
        return of(null, collection);
    }

    public static Completer of(Completer parent, Collection<? extends Runnable> collection) {
        switch (collection.size()) {
            case 0:
                throw new UnsupportedOperationException();
            case 1:
                return new RunnableCompleter(parent, collection.iterator().next());
            default:
                return new MultiRunnableCompleter(parent, collection);
        }
    }

    protected abstract void execute();

    protected boolean isDirectInvoke() {
        return directInvoke;
    }

    public final void invokeDirect() {
        directInvoke = true;
        execute();
        onCompletion(this);
    }

    @Override
    public final void compute() {
        this.execute();
        tryComplete();
    }

    protected void forkNew(Completer completer) {
        if (directInvoke) {
            completer.invokeDirect();
        } else {
            addToPendingCount(1);
            completer.fork();
        }
    }

    protected <Z> void tailCall(Collection<Z> collection, Function<Z, Completer> mapper) {
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

    protected <Z> void tailCall(Z[] collection, Function<Z, Completer> mapper) {
        if (directInvoke) {
            for (Z o : collection) {
                mapper.apply(o).invokeDirect();
            }
        } else {
            for (int i = 0; i < collection.length; i++) {
                Completer c = mapper.apply(collection[i]);
                addToPendingCount(1);
                if (i == 0) {
                    c.compute();
                } else {
                    c.fork();
                }
            }
        }
    }

    private static class MultiRunnableCompleter extends Completer {
        private final Collection<? extends Runnable> collection;

        MultiRunnableCompleter(Completer completer, Collection<? extends Runnable> collection) {
            super(completer);
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
        private final Runnable runnable;

        public RunnableCompleter(Completer completer, Runnable runnable) {
            super(completer);
            this.runnable = runnable;
        }

        @Override
        protected void execute() {
            runnable.run();
        }
    }
}
