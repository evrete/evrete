package org.evrete.examples.misc;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RhsContext;
import org.evrete.api.StatelessSession;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class StatelessSimpleAnnotated {
    public static void main(String[] args) throws IOException {
        KnowledgeService service = new KnowledgeService();

        Knowledge knowledge = service
                .newKnowledge()
                .importRules(
                        "JAVA-CLASS",
                        StatelessSimpleAnnotated.class);

        StatelessSession session = knowledge.newStatelessSession();

        AtomicInteger obj = new AtomicInteger(0);
        System.out.println("Pre-value: " + obj.get());
        session.insertAndFire(obj);
        System.out.println("Post-value: " + obj.get());
        service.shutdown();
    }

    @Rule
    @Where(value = "$ai.get < 10")
    public static void rule(RhsContext ctx, @Fact("$ai") AtomicInteger i) {
        i.incrementAndGet();
        ctx.update(i);
    }

}
