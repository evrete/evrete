package org.evrete.benchmarks.helper;

import org.evrete.api.StatefulSession;
import org.kie.api.event.rule.*;
import org.kie.api.runtime.KieSession;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Predicate;

public final class TestUtils {

    public static long nanoExecTime(Runnable r) {
        long t0 = System.nanoTime();
        r.run();
        return System.nanoTime() - t0;
    }

    private static <T> void deleteFrom(Collection<T> collection, Predicate<T> predicate) {
        LinkedList<T> selected = new LinkedList<>();
        for (T obj : collection) {
            if (predicate.test(obj)) selected.add(obj);
        }

        for (T o : selected) {
            collection.remove(o);
        }
    }

    static Collection<FactEntry> sessionFacts(StatefulSession s) {
        Collection<FactEntry> col = new LinkedList<>();
        s.forEachFact((handle, fact) -> col.add(new FactEntry(handle, fact)));
        return col;
    }

    public static void logAgenda(KieSession dSession) {
        dSession.addEventListener(new AgendaEventListener() {
            @Override
            public void matchCreated(MatchCreatedEvent event) {
                System.out.println("Created: " + event);
            }

            @Override
            public void matchCancelled(MatchCancelledEvent event) {
                System.out.println("Cancelled: " + event);
            }

            @Override
            public void beforeMatchFired(BeforeMatchFiredEvent event) {
                System.out.println("Before fire: " + event);
            }

            @Override
            public void afterMatchFired(AfterMatchFiredEvent event) {
                System.out.println("After fire: " + event);
            }

            @Override
            public void agendaGroupPopped(AgendaGroupPoppedEvent agendaGroupPoppedEvent) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void agendaGroupPushed(AgendaGroupPushedEvent agendaGroupPushedEvent) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent ruleFlowGroupActivatedEvent) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent ruleFlowGroupActivatedEvent) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent ruleFlowGroupDeactivatedEvent) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent ruleFlowGroupDeactivatedEvent) {
                throw new UnsupportedOperationException();
            }
        });
    }
}
