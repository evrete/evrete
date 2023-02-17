package org.evrete.examples.run;

import org.evrete.KnowledgeService;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;

import java.util.List;

import static java.lang.System.out;

public class SelfMutationInline {

    @SuppressWarnings("resource")
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
                    // Appending a new rule
                    sess
                            .newRule(event)
                            .forEach("$i", Integer.class)
                            .where("$i % 2 == 0")
                            .execute(c -> out.printf("%s:\t%s%n", event, c.get("$i")));
                    out.printf("New rule created: '%s'%n", event);
                })
                .newStatefulSession();

        // 1. Inserting session into self
        session.insertAndFire(session);
        // 2. Nothing happens, the rule requires a String to activate
        session.insertAndFire("Rule1");
        // 3. Testing the new rule
        session.insertAndFire(1, 2, 3, 4, 5, 6);
        // 4. Another event
        session.insertAndFire("Rule2");
        // 5. And another test
        session.insertAndFire(1, 2, 3, 4, 5, 6);
        // 6. Listing session's rules
        out.printf("%nSession rules:%n");
        List<RuntimeRule> rules = session.getRules();
        for (int i = 0; i < rules.size(); i++) {
            out.printf("%d\t'%s'%n", i + 1, rules.get(i).getName());
        }

        session.close();
        service.shutdown();
    }
}