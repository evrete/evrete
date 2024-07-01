package org.evrete.examples.run;

import org.evrete.KnowledgeService;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.dsl.annotation.Rule;

import java.util.List;

import static java.lang.System.out;

public class SelfMutationAnnotated {

    public static void main(String[] args) throws Exception {
        KnowledgeService service = new KnowledgeService();
        StatefulSession session = service
                .newKnowledge()
                .importRules("JAVA-CLASS", MutatingRuleset.class)
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

    public static class MutatingRuleset {

        @SuppressWarnings("resource")
        @Rule("Root rule")
        public void rule(StatefulSession sess, String event) {
            // Appending a new rule
            sess
                    .builder()
                    .newRule(event)
                    .forEach("$i", Integer.class)
                    .where("$i % 2 == 0")
                    .execute(c -> evenNumbersAction(event, c.get("$i")))
                    .build();
            out.printf("New rule created: '%s'%n", event);
        }

        void evenNumbersAction(String rule, int i) {
            out.printf("%s:\t%s%n", rule, i);
        }
    }
}
