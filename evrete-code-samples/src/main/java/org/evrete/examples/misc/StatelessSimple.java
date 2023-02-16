package org.evrete.examples.misc;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatelessSession;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class StatelessSimple {
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        withKnowledge(service);
        withoutKnowledge(service);
        service.shutdown();
    }

    private static void withKnowledge(KnowledgeService service) {
        Knowledge knowledge = service
                .newKnowledge()
                .newRule()
                .forEach("$ai", AtomicInteger.class)
                .where("$ai.get() < 10")
                .execute(context -> {
                    AtomicInteger obj = context.get("$ai");
                    obj.incrementAndGet();
                    context.update(obj);
                });

        StatelessSession session = knowledge.newStatelessSession();

        AtomicInteger obj = new AtomicInteger(0);
        System.out.println("Pre-value: " + obj.get()); // Prints 0
        session.insertAndFire(obj);
        System.out.println("Post-value: " + obj.get()); // Prints 10
    }

    private static void withoutKnowledge(KnowledgeService service) {
        StatelessSession session = service
                .newStatelessSession()
                .newRule()
                .forEach("$ai", AtomicInteger.class)
                .where("$ai.get() < 10")
                .execute(context -> {
                    AtomicInteger obj = context.get("$ai");
                    obj.incrementAndGet();
                    context.update(obj);
                });

        AtomicInteger obj = new AtomicInteger(0);
        System.out.println("Pre-value: " + obj.get()); // Prints 0
        session.insertAndFire(obj);
        System.out.println("Post-value: " + obj.get()); // Prints 10
    }
}
