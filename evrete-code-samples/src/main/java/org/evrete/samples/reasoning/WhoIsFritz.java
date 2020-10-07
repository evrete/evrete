package org.evrete.samples.reasoning;

import org.evrete.KnowledgeService;
import org.evrete.api.StatefulSession;

/**
 * A classical forward chaining example from
 * https://en.wikipedia.org/wiki/Forward_chaining
 */
class WhoIsFritz {
    public static void main(String[] args) {

        KnowledgeService service = new KnowledgeService();
        StatefulSession session = service
                .newKnowledge()
                .newRule("rule 1")
                .forEach("$s", Subject.class)
                .where("$s.croaks && $s.eatsFlies")
                .execute(
                        ctx -> {
                            Subject $s = ctx.get("$s");
                            if (!$s.isFrog) {
                                $s.isFrog = true;
                                // To avoid infinite evaluation loop,
                                // update object only when it's changed
                                ctx.update($s);
                            }
                        }
                )
                .newRule("rule 2")
                .forEach("$s", Subject.class)
                .where("$s.chirps && $s.sings")
                .execute(
                        ctx -> {
                            Subject $s = ctx.get("$s");
                            if (!$s.isCanary) {
                                $s.isCanary = true;
                                ctx.update($s);
                            }
                        }
                )
                .newRule("rule 3")
                .forEach("$s", Subject.class)
                .where("$s.isFrog")
                .execute(
                        ctx -> {
                            Subject $s = ctx.get("$s");
                            if (!$s.green) {
                                $s.green = true;
                                ctx.update($s);
                            }
                        }
                )
                .newRule("rule 4")
                .forEach("$s", Subject.class)
                .where("$s.isCanary")
                .execute(
                        ctx -> {
                            Subject $s = ctx.get("$s");
                            if (!$s.yellow) {
                                $s.yellow = true;
                                ctx.update($s);
                            }
                        }
                )
                .createSession();

        // Fritz and his known properties
        Subject fritz = new Subject();
        fritz.eatsFlies = true;
        fritz.croaks = true;

        // Insert Fritz and fire all rules
        session.insertAndFire(fritz);

        // Fritz should have been identified as a green frog
        System.out.println("Is Fritz a frog?\t" + fritz.isFrog);
        System.out.println("Is Fritz green?\t\t" + fritz.green);


        session.close();
        service.shutdown();
    }


    @SuppressWarnings("unused")
    public static class Subject {
        public boolean croaks;
        public boolean eatsFlies;
        public boolean chirps;
        public boolean sings;
        public boolean isFrog;
        public boolean isCanary;
        public boolean green;
        public boolean yellow;
    }
}
