package org.evrete.showcase.abs.town;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.runtime.ConditionNodeDescriptor;
import org.evrete.runtime.memory.BetaMemoryNode;
import org.evrete.showcase.abs.town.types.Entity;
import org.evrete.showcase.abs.town.types.WorldTime;

import java.util.function.Consumer;

public class Tmp {

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        try {
            main(service);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            service.shutdown();
        }

    }

    private static void main(KnowledgeService service) throws Exception {
        Knowledge knowledge = service.newKnowledge();

        knowledge.addConditionTestListener(new EvaluationListenerOld() {
            @Override
            public void apply(BetaMemoryNode<ConditionNodeDescriptor> node, Evaluator evaluator, IntToValue values, boolean result) {
                System.out.println("!!!!!!!! " + evaluator);
            }
        });

        TypeResolver defaultResolver = knowledge.getTypeResolver();
        Type<Entity> entityType = defaultResolver.declare(Entity.class);
        defaultResolver.wrapType(new Entity.EntityKnowledgeType(entityType));

        StatefulSession session = knowledge
                .newRule("Working persons wakeup")
                .forEach(
                        "$person", Entity.class,
                        "$time", WorldTime.class
                )
                .where("$person.properties.location == $person.properties.home")
                //.where("$time.seconds > $person.numbers.wakeup")
                .execute(new Consumer<RhsContext>() {
                    @Override
                    public void accept(RhsContext ctx) {
                        Entity $person = ctx.get("$person");
                        $person.set("location", null);
                        ctx.update($person);
                        System.out.println("!!! Wakeup ");
                    }
                })

                .newRule("Working persons, arrival at work")
                .forEach(
                        "$person", Entity.class,
                        "$time", WorldTime.class
                )
                .where("$person.properties.location == null")
                //.where("$time.absoluteTimeSeconds - $person.numbers.transit_start_time > 1200") // 20 min
                .execute(new Consumer<RhsContext>() {
                    @Override
                    public void accept(RhsContext ctx) {
                        Entity $person = ctx.get("$person");
                        $person.set("transit", false);
                        Entity workPlace = $person.getProperty("work");
                        if (workPlace == null) {
                            throw new IllegalStateException();
                        }
                        $person.set("location", workPlace);
                        ctx.update($person);
                        System.out.println("!!! Arrived ");
                    }
                }).createSession();


        WorldTime worldTime = new WorldTime();

        Entity person = new Entity("person");
        Entity home = new Entity("home");
        Entity work = new Entity("work");

        person.set("work", work);
        person.set("home", home);
        person.set("location", home);


        session.insert(person);
        session.insert(worldTime);

        System.out.println("Initial");
        session.fire();
        System.out.println("Time update");
        session.update(worldTime.increment(60 * 5));
        session.fire();


/*
        int tmp = 0;
        while (worldTime.absoluteTimeSeconds() - worldTime.getInitialTimeSeconds() < 3600 * 24 * 7) {
            session.update(worldTime.increment(60 * 5));
            System.out.println("updating time.....");
            session.fire();
            if (tmp++ > 20) {
                break;
            }
        }
*/


    }
}
