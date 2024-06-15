package org.evrete.runtime;

import org.evrete.collections.LongObjectHashMap;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Function;

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
    private final LongObjectHashMap<State> states;


    //TODO config option
    public WorkMemoryActionBuffer() {
        this.states = new LongObjectHashMap<>(8192);
    }

    State getState(DefaultFactHandle handle) {
        return states.get(handle.getId());
    }

    private WorkMemoryActionBuffer(LongObjectHashMap<State> states) {
        this.states = states;
    }

    public void add(DeltaMemoryAction.Delete action) {
        this.computeIfAbsent(action.getHandle(), k -> new State()).apply(action);
    }

    public void add(DeltaMemoryAction.Insert action) {
        this.computeIfAbsent(action.getHandle(), k -> new State()).apply(action);
    }

    private State computeIfAbsent(DefaultFactHandle handle, Function<DefaultFactHandle, State> function) {
        long id = handle.getId();
        State state = this.states.get(id);
        if (state == null) {
            synchronized (states) {
                state = this.states.get(id);
                if (state == null) {
                    state = function.apply(handle);
                    this.states.put(id, state);
                }
            }
        }
        return state;
    }

    synchronized SplitView sinkToSplitView() {
        SplitView splitView = new SplitView();
        this.states.values().forEach(state -> {
            if(state.lastInsert != null) {
                splitView.add(state.lastInsert);
            }
            if(state.firstDelete != null) {
                splitView.add(state.firstDelete);
            }
        });
        this.states.clear();
        return splitView;
    }

    public void addMultiple(Collection<? extends DeltaMemoryAction.Insert> actions) {
        for (DeltaMemoryAction.Insert action : actions) {
            this.add(action);
        }
    }

    @Override
    public String toString() {
        return states.toString();
    }

    public boolean hasData() {
        return this.states.size() > 0;
    }

    public int size() {
        return states.size();
    }

    public void clear() {
        this.states.clear();
    }

    /**
     * Clears local tasks by moving them into a copy of self.
     * @return new copy of currently tasks
     */
    public synchronized WorkMemoryActionBuffer sinkToNew() {
        LongObjectHashMap<State> copy = new LongObjectHashMap<>(this.states);
        clear();
        return new WorkMemoryActionBuffer(copy);
    }

    public synchronized void sinkTo(WorkMemoryActionBuffer other) {
        this.states.values().forEach(state -> {
            if(state.lastInsert != null) {
                other.add(state.lastInsert);
            }
            if(state.firstDelete != null) {
                other.add(state.firstDelete);
            }
        });
        this.states.clear();
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

        void apply(DeltaMemoryAction.Insert action) {
            this.lastInsert = Objects.requireNonNull(action);
        }

        void apply(DeltaMemoryAction.Delete action) {
            if(firstDelete == null) {
                this.firstDelete = Objects.requireNonNull(action);
            }
        }

        void apply(State other) {
            if(other.lastInsert != null) {
                this.apply(other.lastInsert);
            }

            if(other.firstDelete != null) {
                this.apply(other.firstDelete);
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
}
