package org.evrete.samples;

import org.evrete.KnowledgeService;
import org.evrete.api.RhsContext;
import org.evrete.api.StatefulSession;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

import java.io.IOException;

public class PrimeNumbersDSLClass {
    public static void main(String[] args) throws IOException {
        KnowledgeService service = new KnowledgeService();
        StatefulSession session = service
                .newKnowledge()
                .appendDslRules(
                        "JAVA-CLASS",
                        PrimeNumbersDSLClass.class
                )
                .createSession();

        // Inject candidates
        for (int i = 2; i <= 100; i++) {
            session.insert(i);
        }
        // Execute rules
        session.fire();
        // Printout current memory state
        session.forEachFact((handle, o) -> System.out.print(o + " "));
        // Closing resources
        session.close();
        service.shutdown();
    }

    @Rule
    @Where(value = "$i1 * $i2 == $i3")
    public static void rule(RhsContext ctx, @Fact("$i1") int i1, @Fact("$i2") int i2, @Fact("$i3") int i3) {
        ctx.deleteFact("$i3");
    }
}
