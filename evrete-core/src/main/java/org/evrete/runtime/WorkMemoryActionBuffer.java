package org.evrete.runtime;

import org.evrete.collections.LongKeyMap;
import org.evrete.util.CommonUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Contains a buffer of memory actions per fact handle. This buffer is used in two ways:
 * <ul>
 *   <li>
 *     Each session has its own {@link WorkMemoryActionBuffer} for registering changes resulting
 *     from <code>insert</code>, <code>update</code>, or <code>delete</code> operations performed
 *     on the session's instance externally.
 *   </li>
 *   <li>
 *     The same actions performed inside a rule's action are also buffered in the rule's internal storage
 *     and then applied to the provided {@link WorkMemoryActionBuffer}.
 *   </li>
 * </ul>
 *
 * <p>
 * When the engine decides to apply a buffer to the session's memory, the delete operations are executed first.
 * After that, executing insert operations will produce the memory's delta state
 * (see the {@link org.evrete.api.spi.DeltaInsertMemory} description).
 * </p>
 * <p>
 * As the engine's memory structures do not support any update operations, it is this buffer's responsibility to
 * convert update operations into a valid combination of a delete operation and an insert operation.
 * </p>
 */
class WorkMemoryActionBuffer {
    static WorkMemoryActionBuffer EMPTY = new WorkMemoryActionBuffer();
    private final LongKeyMap<OrderedFuture> pendingActions = new LongKeyMap<>();
    private final AtomicLong actionOrderedCounter = new AtomicLong();

    void addInsert(ActiveType type, boolean applyToStorage, CompletableFuture<RoutedFactHolder> factHolder) {
        addPendingFuture(
                factHolder
                        .thenApply(holder -> new DeltaMemoryAction.Insert(type, holder.getFactHolder().getHandle(), applyToStorage, holder))
        );
    }

    void addDelete(ActiveType type, boolean applyToStorage, FactHolder deleteSubject) {
        addPendingFuture(CompletableFuture.completedFuture(new DeltaMemoryAction.Delete(type, applyToStorage, deleteSubject)));
    }

    private synchronized void addPendingFuture(CompletableFuture<DeltaMemoryAction> future) {
        long order = actionOrderedCounter.incrementAndGet();
        OrderedFuture previous = this.pendingActions.put(order, new OrderedFuture(future, order));
        if (previous != null) {
            throw new IllegalStateException("Previous future is not null. File a bug report.");
        }
    }

    synchronized CompletableFuture<SplitView> sinkToSplitView() {
        CompletableFuture<List<OrderedAction>> completedActions = CommonUtils.completeAndCollect(
                        this.pendingActions,
                        orderedFuture -> orderedFuture.future.thenApply(action -> new OrderedAction(action, orderedFuture))
                )
                .thenApply(this::clearPendingActions);

        return completedActions.thenApply(this::sinkToSplitViewSync);
    }

    private List<OrderedAction> clearPendingActions(List<OrderedAction> actions) {
        for (OrderedAction orderedAction : actions) {
            pendingActions.remove(orderedAction.source.order);
        }
        actionOrderedCounter.set(0);
        //TODO test & optimize. Create some write locks or something.
        if (pendingActions.size() > 0) {
            throw new IllegalStateException("PendingActions not empty. File a bug report");
        }
        return actions;
    }

    SplitView sinkToSplitViewSync(List<OrderedAction> actions) {
        // Sort actions by their order of appearance
        actions.sort(Comparator.comparingLong(o -> o.source.order));

        // Collect actions by their fact handles
        LongKeyMap<State> states = new LongKeyMap<>();
        actions.forEach(action -> states.computeIfAbsent(action.action.getHandle().getId(), State::new).apply(action.action));

        SplitView splitView = new SplitView();
        states.forEach(state -> {
            if (state.lastInsert != null) {
                splitView.add(state.lastInsert);
            }
            if (state.firstDelete != null) {
                splitView.add(state.firstDelete);
            }
        });

        return splitView;
    }

    public boolean hasData() {
        return bufferedActionCount() > 0;
    }

    public int bufferedActionCount() {
        return this.pendingActions.size();
    }

    static class SplitView {
        private final Collection<DeltaMemoryAction.Insert> inserts = new LinkedList<>();
        private final Collection<DeltaMemoryAction.Delete> deletes = new LinkedList<>();

        void add(DeltaMemoryAction.Insert action) {
            this.inserts.add(action);
        }

        void add(DeltaMemoryAction.Delete action) {
            this.deletes.add(action);
        }

        public Collection<DeltaMemoryAction.Insert> getInserts() {
            return inserts;
        }

        public Collection<DeltaMemoryAction.Delete> getDeletes() {
            return deletes;
        }
    }

    static class State {
        /**
         * Last insert with related to the fact handle.
         * The latest insert contains the most recent data
         */
        DeltaMemoryAction.Insert lastInsert;
        /**
         * First delete with related to the fact handle
         */
        DeltaMemoryAction.Delete firstDelete;

        private void applyInsert(DeltaMemoryAction.Insert action) {
            this.lastInsert = Objects.requireNonNull(action);
        }

        private void applyDelete(DeltaMemoryAction.Delete action) {
            if (firstDelete == null) {
                this.firstDelete = Objects.requireNonNull(action);
            }
        }

        void apply(DeltaMemoryAction action) {
            if (action instanceof DeltaMemoryAction.Insert) {
                applyInsert((DeltaMemoryAction.Insert) action);
            } else if (action instanceof DeltaMemoryAction.Delete) {
                applyDelete((DeltaMemoryAction.Delete) action);
            } else {
                throw new IllegalStateException("Unexpected action type: " + action.getClass());
            }
        }

        @Override
        public String toString() {
            return "{" +
                    "lastInsert=" + lastInsert +
                    ", firstDelete=" + firstDelete +
                    '}';
        }
    }

    static class OrderedFuture {
        final CompletableFuture<DeltaMemoryAction> future;
        final long order;

        public OrderedFuture(CompletableFuture<DeltaMemoryAction> future, long order) {
            this.future = future;
            this.order = order;
        }
    }

    static class OrderedAction {
        final DeltaMemoryAction action;
        final OrderedFuture source;

        public OrderedAction(DeltaMemoryAction future, OrderedFuture source) {
            this.action = future;
            this.source = source;
        }
    }
}
