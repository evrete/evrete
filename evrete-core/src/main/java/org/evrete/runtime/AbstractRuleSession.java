package org.evrete.runtime;

import org.evrete.api.ActivationMode;
import org.evrete.api.RuleSession;
import org.evrete.api.events.SessionCreatedEvent;
import org.evrete.api.events.SessionFireEvent;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * <p>
 * Base session class with common methods
 * </p>
 *
 * @param <S> session type parameter
 */
public abstract class AbstractRuleSession<S extends RuleSession<S>> extends AbstractRuleSessionDeployment<S> {
    private static final Logger LOGGER = Logger.getLogger(AbstractRuleSession.class.getName());
    private final SessionMemory memory;

    AbstractRuleSession(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.memory = new SessionMemory(this);
        // Deploy existing rules
        deployRules(knowledge.getRuleDescriptors(), false);
        // Publish the Session Created event
        broadcast(SessionCreatedEvent.class, () -> AbstractRuleSession.this);
    }

    @Override
    public final SessionMemory getMemory() {
        return memory;
    }

    void clearInner() {
        for (SessionRule rule : ruleStorage) {
            rule.clear();
        }
        memory.clear();
        this.getActionBuffer().clear();
    }

    final void fireInner() {
        broadcast(SessionFireEvent.class, () -> AbstractRuleSession.this);
        ActivationMode mode = getAgendaMode();
        WorkMemoryActionBuffer buffer = getActionBuffer().sinkToNew();
        ActivationContext context = new ActivationContext(
                getMemory(), // Session memory
                getService().getExecutor(),
                ruleStorage.getList(), // Current rules
                buffer // Copy of currently buffered tasks
        );

        LOGGER.fine(()->"START, session mode: " + mode + ", buffered facts: [" + buffer.size() + "]");
        switch (mode) {
            case DEFAULT:
                fireDefault(context);
                break;
            case CONTINUOUS:
                fireContinuous(context);
                break;
            default:
                throw new IllegalStateException("Unknown mode " + getAgendaMode());
        }
        LOGGER.fine(()->"END");
    }

    private void fireDefault(ActivationContext ctx) {
        List<SessionRule> agenda;
        while (ctx.hasPendingTasks()) {
            // Compute rules to fire
            agenda = ctx.computeAgenda();
            WorkMemoryActionBuffer destinationForRuleActions = ctx.getMemoryTasks();
            if (!agenda.isEmpty()) {
                // Report the agenda to the activation manager
                activationManager.onAgenda(ctx.incrementFireCount(), Collections.unmodifiableList(agenda));
                for (SessionRule rule : agenda) {
                    if (activationManager.test(rule)) {
                        // The rule is allowed for activation
                        RhsResultReducer actionResult = rule.callRhs();
                        activationManager.onActivation(rule, actionResult.getActivationCount());
                        // Sink the RHS actions (inserts, updates, deletes called from inside the RHS)
                        // into pending tasks
                        actionResult.getActionBuffer().sinkTo(destinationForRuleActions);
                    }
                }
            }
            ctx.commitDeltaMemories();
        }
    }

    private void fireContinuous(ActivationContext ctx) {
        List<SessionRule> agenda;
        while (ctx.hasPendingTasks()) {
            agenda = ctx.computeAgenda();
            WorkMemoryActionBuffer destinationForRuleActions = new WorkMemoryActionBuffer();
            if (!agenda.isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), Collections.unmodifiableList(agenda));
                for (SessionRule rule : agenda) {
                    if (activationManager.test(rule)) {
                        RhsResultReducer actionResult = rule.callRhs();
                        activationManager.onActivation(rule, actionResult.getActivationCount());
                        actionResult.getActionBuffer().sinkTo(destinationForRuleActions);
                    }
                }
            }
            destinationForRuleActions.sinkTo(ctx.getMemoryTasks()); // Do we need that buff var?
            ctx.commitDeltaMemories();
        }
    }
}
