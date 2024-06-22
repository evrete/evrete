package org.evrete.runtime;

import org.evrete.collections.LongKeyMap;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

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
    private final LongKeyMap<State> actionsPerFactHandle;

    private WorkMemoryActionBuffer(LongKeyMap<State> actionsPerFactHandle) {
        this.actionsPerFactHandle = actionsPerFactHandle;
    }

    public WorkMemoryActionBuffer() {
        this(new LongKeyMap<>());
    }

    private WorkMemoryActionBuffer(WorkMemoryActionBuffer other) {
        this(new LongKeyMap<>(other.actionsPerFactHandle));
    }


    private void clear() {
        this.actionsPerFactHandle.clear();
    }

    void addInsert(DeltaMemoryAction.Insert insertOp) {
        this.actionsPerFactHandle.computeIfAbsent(insertOp.getHandle().getId(), ()->new State(insertOp.getType())).applyInsert(insertOp);
    }

    void addDelete(DeltaMemoryAction.Delete deleteOp) {
        this.actionsPerFactHandle.computeIfAbsent(deleteOp.getHandle().getId(), () -> new State(deleteOp.getType())).applyDelete(deleteOp);
    }



//    synchronized CompletableFuture<SplitView> sinkToSplitView() {
//        return completionFuture().thenApply(new Function<Void, SplitView>() {
//            @Override
//            public SplitView apply(Void unused) {
//                SplitView splitView = sinkToSplitViewSync();
//                clear();
//                return splitView;
//            }
//        });
//    }

//    private List<OrderedAction> clearPendingActions(List<OrderedAction> actions) {
//        for (OrderedAction orderedAction : actions) {
//            pendingActions.remove(orderedAction.source.order);
//        }
//        actionOrderedCounter.set(0);
//        //TODO test & optimize. Create some write locks or something.
//        if (pendingActions.size() > 0) {
//            throw new IllegalStateException("PendingActions not empty. File a bug report");
//        }
//        return actions;
//    }

    // TODO the executor isn't actually used
    CompletableFuture<Collection<SplitView>> sinkToSplitView(ExecutorService executor) {
        return CompletableFuture.supplyAsync(new Supplier<Collection<SplitView>>() {
            @Override
            public Collection<SplitView> get() {
                return sinkToSplitViewSync();
            }
        }, executor);

    }


    private Collection<SplitView> sinkToSplitViewSync() {
        Map<ActiveType, SplitView> map = new HashMap<>();


//        // Sort actions by their order of appearance
//        actions.sort(Comparator.comparingLong(o -> o.source.order));
//
//        // Collect actions by their fact handles
//        LongKeyMap<State> states = new LongKeyMap<>();
//        actions.forEach(action -> states.computeIfAbsent(action.action.getHandle().getId(), State::new).apply(action.action));

        //SplitView splitView = new SplitView();
        actionsPerFactHandle.forEach(state -> {

            SplitView splitView = map.computeIfAbsent(state.type, SplitView::new);

            // 2. Apply the computed states to the resulting view
            if (state.lastInsert != null) {
                splitView.add(state.lastInsert);
            }
            if (state.firstDelete != null) {
                splitView.add(state.firstDelete);
            }
        });

        this.clear();
        return map.values();
    }

    public boolean hasData() {
        return bufferedActionCount() > 0;
    }

    public int bufferedActionCount() {
        return this.actionsPerFactHandle.size();
    }

    static class SplitView {
        private final ActiveType type;
        private final Collection<DeltaMemoryAction.Insert> inserts = new LinkedList<>();
        private final Collection<DeltaMemoryAction.Delete> deletes = new LinkedList<>();

        public SplitView(ActiveType type) {
            this.type = type;
        }

        public ActiveType getType() {
            return type;
        }

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

        final ActiveType type;

        State(ActiveType type) {
            this.type = type;
        }

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

    static class FactHandleAction {
        static final Comparator<FactHandleAction> COMPARATOR = Comparator.comparingLong(o -> o.sequence);
        final DeltaMemoryAction action;
        final long sequence;

        FactHandleAction(DeltaMemoryAction action, long sequence) {
            this.action = action;
            this.sequence = sequence;
        }
    }


}
