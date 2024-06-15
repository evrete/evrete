package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.api.events.ConditionEvaluationEvent;
import org.evrete.collections.ForkingArray;
import org.evrete.runtime.evaluation.DefaultEvaluatorHandle;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * This class holds the conditions of the context and assigns them handles (see {@link EvaluatorHandle}).
 */
public class ActiveEvaluatorGenerator implements Copyable<ActiveEvaluatorGenerator>, EvaluatorsContext {
    private final ForkingArray<StoredCondition> valuePredicates;

    private final AtomicLong counter;
    // Executor is required for evaluator publishers
    private final Executor executor;

    ActiveEvaluatorGenerator(Executor executor) {
        this.executor = executor;
        this.valuePredicates = new ForkingArray<>(1024);
        this.counter = new AtomicLong();
    }

    private ActiveEvaluatorGenerator(ActiveEvaluatorGenerator other) {
        this.executor = other.executor;
        this.counter = new AtomicLong(other.counter.get());
        this.valuePredicates = other.valuePredicates.newBranch();
    }

    @Override
    public ValuesPredicate getPredicate(EvaluatorHandle handle) {
        StoredCondition wrapper = get((DefaultEvaluatorHandle) handle, true);
        return wrapper == null ? null : wrapper.getCondition();
    }

    @Override
    public void replacePredicate(EvaluatorHandle handle, final ValuesPredicate newPredicate) {
        replace((DefaultEvaluatorHandle) handle, newPredicate);
    }

    /**
     * <p>
     * Registers new condition and returns its handle.
     * </p>
     *
     * @param predicate condition to add
     * @return new {@link EvaluatorHandle} or the one of an existing condition.
     */
    public synchronized DefaultEvaluatorHandle addEvaluator(ValuesPredicate predicate, double complexity, LhsField.Array<String, ActiveField> fields) {
        return this.valuePredicates.stream(true)
                .filter(arg -> arg.getCondition().equals(predicate))
                .map(StoredCondition::getHandle)
                .findAny()
                .orElseGet(() -> {
                    StoredCondition stored = valuePredicates.append(predicate, (idx, p) -> new StoredCondition(idx, p, complexity, fields));
                    return stored.getHandle();
                });
    }

    @Override
    public Events.Publisher<ConditionEvaluationEvent> publisher(EvaluatorHandle handle) {
        return get((DefaultEvaluatorHandle) handle, false).getCreatePublisher(this.executor);
    }

    @Override
    public void forEach(BiConsumer<EvaluatorHandle, ValuesPredicate> consumer) {
        this.valuePredicates.stream(false)
                .forEach(
                        storedCondition -> consumer.accept(
                                storedCondition.getHandle(),
                                storedCondition.getCondition()
                        )
                );
    }

//    void replace(DefaultEvaluatorHandle handle, Evaluator evaluator) {
//        StoredCondition existing = get(handle, false);
//        FieldReference[] d1 = existing.sourceEvaluator().descriptor();
//        FieldReference[] d2 = evaluator.descriptor();
//        if (FieldReference.sameDescriptors(d1, d2)) {
//            existing.setDelegate(evaluator);
//        } else {
//            throw new IllegalArgumentException("Mismatched descriptors");
//        }
//    }

    private void replace(DefaultEvaluatorHandle handle, ValuesPredicate predicate) {
        get(handle, false).setPredicate(predicate);
    }

//    synchronized ActiveEvaluator addEvaluatorToCurrentMap(Evaluator evaluator, ActiveField[] activeFields) {
//        // Look for matching evaluator
//        Optional<Map.Entry<DefaultEvaluatorHandle, ActiveEvaluator>> found = this.allEntries()
//                .filter(entry -> {
//                    ActiveEvaluator existing = entry.getValue();
//                    return existing.sourceEvaluator().equals(evaluator)
//                            &&
//                            Arrays.equals(activeFields, existing.activeFields())
//                            ;
//                })
//                .findFirst();
//        if (found.isPresent()) {
//            return found.get().getValue();
//        } else {
//            DefaultEvaluatorHandle handle = new DefaultEvaluatorHandle(counter.incrementAndGet(), evaluator.getComplexity());
//            ActiveEvaluator activeEvaluator = new ActiveEvaluator(handle, evaluator, activeFields);
//            this.put(handle, activeEvaluator);
//            return activeEvaluator;
//        }
//    }

    public StoredCondition get(DefaultEvaluatorHandle handle, boolean returnNull) {
        StoredCondition result = this.valuePredicates.get(handle.getIndex());
        if (result == null) {
            if (returnNull) {
                return null;
            } else {
                throw new IllegalArgumentException("Unknown evaluator " + handle);
            }
        } else {
            return result;
        }
    }

    @Override
    public ActiveEvaluatorGenerator copyOf() {
        return new ActiveEvaluatorGenerator(this);
    }

}
