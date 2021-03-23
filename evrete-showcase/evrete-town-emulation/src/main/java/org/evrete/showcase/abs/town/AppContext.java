package org.evrete.showcase.abs.town;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.showcase.abs.town.json.GeoData;
import org.evrete.showcase.abs.town.types.Entity;
import org.evrete.showcase.abs.town.types.RandomUtils;
import org.evrete.showcase.abs.town.types.World;
import org.evrete.showcase.abs.town.types.WorldTime;
import org.evrete.showcase.shared.Utils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.function.Consumer;


@WebListener
public class AppContext implements ServletContextListener {
    static GeoData MAP_DATA;
    private static KnowledgeService knowledgeService;
    private static Knowledge knowledge;

    private static KnowledgeService knowledgeService() {
        if (knowledgeService == null) {
            throw new IllegalStateException();
        } else {
            return knowledgeService;
        }
    }

    static Knowledge knowledge() {
        return knowledge;
    }

    private static Knowledge buildKnowledge() {
        Knowledge knowledge = knowledgeService().newKnowledge();
        TypeResolver defaultResolver = knowledge.getTypeResolver();
        Type<Entity> entityType = defaultResolver.declare(Entity.class);
        defaultResolver.wrapType(new Entity.EntityKnowledgeType(entityType));


        FactBuilder[] facts = new FactBuilder[]{
                FactBuilder.fact("$person", Entity.class),
                FactBuilder.fact("$time", WorldTime.class),
                FactBuilder.fact("$world", World.class)
        };

        return knowledge
                .newRule("Wakeup")
                .forEach(facts)
                .where("$person.properties.current_location == $person.properties.home")
                .where("$time.secondsSinceStart > $person.numbers.wakeup")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        int nextWakeUp = person.getNumber("wakeup", -1) + 3600 * 24;
                        // New day, resetting yesterday's planning flags
                        // and setting new wakeup time for tomorrow
                        person
                                .set("wakeup", nextWakeUp)
                                .set("day_planning", true) // Start planning
                        ;
                    }
                })

                .newRule("Planning. Will we shop today?")
                .forEach(facts)
                .where("$person.flags.day_planning == true")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        boolean shopping = RandomUtils.randomBoolean(world.getShoppingProbability());
                        person
                                .set("day_planning", false) // End of planning
                                .set("shopping", shopping)
                        ;
                    }
                })


                .newRule("Non-working persons: day planning")
                .forEach(facts)
                .where("$person.properties.work == null")
                .where("$person.properties.current_location == $person.properties.home")
                .where("$person.flags.shopping == true")
                .where("$person.properties.selected_shop == null")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        Entity selectedShop = world.randomBusiness();
                        int selectedShopTime = time.secondsSinceStart() + world.randomGaussian(2 * 3600, 3 * 3600, 10 * 3600); // Within 6 hours
                        person
                                .set("selected_shop", selectedShop)
                                .set("shopping", false)
                                .set("selected_shop_time", selectedShopTime)
                        ;
                    }
                })


                .newRule("Working persons: commute to work")
                .forEach(facts)
                .where("$person.properties.work != null")
                .where("$person.properties.travel_to == null")
                .where("$person.properties.current_location == $person.properties.home")
                .where("$time.secondsSinceStart > $person.numbers.wakeup")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        person
                                .set("travel_to", person.getProperty("work"))
                        ;
                    }
                })

                .newRule("Working persons: arrival at work")
                .forEach(facts)
                .where("$person.properties.work != null")
                .where("$person.properties.current_location == $person.properties.work")
                .where("$person.flags.just_arrived == true")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        person
                                .set("end_of_work_time", time.secondsSinceStart() + world.randomGaussian(8 * 3600, 2 * 3600, 12 * 3600))
                                .set("just_arrived", false)
                        ;
                    }
                })

                .newRule("Working persons: end of work (home)")
                .forEach(facts)
                .where("$person.properties.work != null")
                .where("$person.properties.current_location == $person.properties.work")
                .where("$person.properties.travel_to == null")
                .where("$time.secondsSinceStart > $person.numbers.end_of_work_time")
                .where("$person.flags.shopping == false")
                .where("$person.flags.just_arrived == false")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        person
                                .set("travel_to", person.getProperty("home"))
                        ;
                    }
                })

                .newRule("Working persons: end of work (shopping)")
                .forEach(facts)
                .where("$person.properties.work != null")
                .where("$person.properties.current_location == $person.properties.work")
                .where("$person.properties.travel_to == null")
                .where("$time.secondsSinceStart > $person.numbers.end_of_work_time")
                .where("$person.flags.shopping == true")
                .where("$person.flags.just_arrived == false")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        Entity selectedShop = world.randomBusiness();
                        person
                                .set("selected_shop", selectedShop)
                                .set("travel_to", selectedShop)
                        ;
                    }
                })


                .newRule("Commute start")
                .forEach(facts)
                .where("$person.properties.travel_to != null")
                .where("$person.properties.current_location != null")
                .where("$person.flags.in_transit == false")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        Entity current = person.getProperty("current_location");
                        person
                                .set("travel_from", current)
                                .set("current_location", null)
                                .set("in_transit", true)
                                .set("arrival_time", time.secondsSinceStart() + 60 * 20) // 20 min travel time
                        ;
                    }
                })

                .newRule("Commute end")
                .forEach(facts)
                .where("$person.properties.current_location == null")
                .where("$person.flags.in_transit == true")
                .where("$time.secondsSinceStart >= $person.numbers.arrival_time")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        Entity destination = person.getProperty("travel_to");
                        person
                                .set("in_transit", false)
                                .set("just_arrived", true)
                                .set("current_location", destination)
                                .set("travel_to", null)
                        ;

                    }
                })

                .newRule("All persons: Shopping time")
                .forEach(facts)
                .where("$person.properties.selected_shop != null")
                .where("$person.properties.current_location != $person.properties.selected_shop")
                .where("$person.properties.travel_to == null")
                .where("$time.secondsSinceStart >= $person.numbers.selected_shop_time")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        person
                                .set("travel_to", person.getProperty("selected_shop"))
                        ;
                    }
                })

                .newRule("All persons: shopping arrival")
                .forEach(facts)
                .where("$person.properties.current_location == $person.properties.selected_shop")
                .where("$person.properties.selected_shop != null")
                .where("$person.flags.just_arrived == true")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        int shoppingDuration = world.randomGaussian(3600, 3600, 4 * 3600);
                        int shoppingEndTime = time.secondsSinceStart() + shoppingDuration;
                        person
                                .set("just_arrived", false)
                                .set("shopping_end_time", shoppingEndTime)
                        ;
                    }
                })


                .newRule("All persons: shopping end")
                .forEach(facts)
                .where("$person.properties.selected_shop != null")
                .where("$person.properties.current_location == $person.properties.selected_shop")
                .where("$person.flags.just_arrived == false")
                .where("$time.secondsSinceStart >= $person.numbers.shopping_end_time")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        person
                                .set("travel_to", person.getProperty("home"))
                                .set("selected_shop", null)
                        ;
                    }
                })
                ;

    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (knowledgeService == null) {
            Configuration configuration = new Configuration();
            knowledgeService = new KnowledgeService(configuration);
            knowledge = buildKnowledge();

            ServletContext ctx = sce.getServletContext();

            try {
                MAP_DATA = Utils.fromJson(
                        Utils.readResourceAsString(ctx, "/WEB-INF/data.json"),
                        GeoData.class
                );
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalStateException("Can not read configuration data", e);
            }

        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (knowledgeService == null) {
            throw new IllegalStateException();
        } else {
            knowledgeService.shutdown();
        }
    }

    private abstract static class PersonTimeConsumer implements Consumer<RhsContext> {
        abstract void process(Entity person, WorldTime time, World world);

        @Override
        public void accept(RhsContext ctx) {
            Entity person = ctx.get("$person");
            WorldTime time = ctx.get("$time");
            World world = ctx.get("$world");
            process(person, time, world);
            ctx.update(person);
        }
    }
}