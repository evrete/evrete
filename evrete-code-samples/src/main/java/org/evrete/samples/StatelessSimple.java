package org.evrete.samples;

import org.evrete.KnowledgeService;
import org.evrete.api.StatelessSession;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class StatelessSimple {
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();

        StatelessSession session = service
                .newStatelessSession()
                .newRule()
                .forEach("$ai", AtomicInteger.class)
                .where("$ai.get < 10")
                .execute(ctx -> {
                    AtomicInteger i = ctx.get("$ai");
                    i.incrementAndGet();
                    ctx.update(i);
                });

        AtomicInteger obj = new AtomicInteger(0);
        System.out.println("Pre-value: " + obj.get());
        session.insertAndFire(obj);
        System.out.println("Post-value: " + obj.get());
        service.shutdown();
    }
}
