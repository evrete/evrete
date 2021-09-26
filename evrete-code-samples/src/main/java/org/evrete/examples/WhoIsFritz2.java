package org.evrete.examples;

import org.evrete.KnowledgeService;
import org.evrete.api.StatefulSession;

/**
 * A classical forward chaining example from
 * https://en.wikipedia.org/wiki/Forward_chaining
 */
class WhoIsFritz2 {
    public static void main(String[] args) {

        KnowledgeService service = new KnowledgeService();
        StatefulSession session = service
                .newKnowledge()
                .newRule()
                .forEach("$s", Subject.class)
                .where("$s.isFrog").where("!$s.green")
                .execute(ctx -> {
                    Subject $s = ctx.get("$s");
                    $s.green = true;
                    ctx.update($s);
                })
                .newRule()
                .forEach("$s", Subject.class)
                .where("$s.croaks").where("$s.eatsFlies").where("!$s.isFrog")
                .execute(ctx -> {
                    Subject $s = ctx.get("$s");
                    $s.isFrog = true;
                    ctx.update($s);
                })
                .newStatefulSession();

        // Fritz and his known properties
        Subject fritz = new Subject();
        fritz.croaks = true;
        fritz.eatsFlies = true;

        // Insert Fritz and fire all rules
        session.insertAndFire(fritz);

        // Fritz should have been identified as a green frog
        System.out.println("Is Fritz a frog?\t" + fritz.isFrog);
        System.out.println("Is Fritz green?\t\t" + fritz.green);

        session.close();
        service.shutdown();
    }


    @SuppressWarnings({"unused", "WeakerAccess"})
    public static class Subject {
        public boolean croaks;
        public boolean eatsFlies;
        public boolean isFrog;
        public boolean green;
    }
}
