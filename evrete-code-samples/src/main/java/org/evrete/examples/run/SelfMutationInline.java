package org.evrete.examples.run;

import org.evrete.KnowledgeService;
import org.evrete.api.StatefulSession;

import static java.lang.System.out;
public class SelfMutationInline {

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        StatefulSession session = service
                .newKnowledge()
                .newRule("Root rule")
                .forEach(
                        "$s", StatefulSession.class,
                        "$e", String.class
                )
                .execute(ctx -> {
                    StatefulSession sess = ctx.get("$s");
                    String event = ctx.get("$e");
                    // Appending new rule with the event's name
                    sess
                            .newRule(event)
                            .forEach("$i", Integer.class)
                            .where("$i % 2 == 0")
                            .execute(c-> {
                                out.println(event + ":\t" + c.get("$i"));
                            });
                    out.println("New rule created: '" + event + "'");
                })
                .newStatefulSession();

        // 1. Inserting session into self
        session.insertAndFire(session);
        // 2. Nothing happens, the rule requires a String to activate
        session.insertAndFire("Rule1");
        // 3. Testing the new rule
        session.insertAndFire(0,1,2,3,4,5,6);
        // 4. Another event
        session.insertAndFire("Rule2");
        // 5. And another test
        session.insertAndFire(0,1,2,3,4,5,6);

        service.shutdown();
    }
}