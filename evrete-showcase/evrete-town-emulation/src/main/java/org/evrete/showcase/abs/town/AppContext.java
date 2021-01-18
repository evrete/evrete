package org.evrete.showcase.abs.town;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.showcase.abs.town.json.GeoData;
import org.evrete.showcase.abs.town.types.Entity;
import org.evrete.showcase.abs.town.types.World;
import org.evrete.showcase.abs.town.types.WorldTime;
import org.evrete.showcase.shared.Utils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.StringReader;
import java.util.function.Consumer;


@WebListener
public class AppContext implements ServletContextListener {
    static GeoData MAP_DATA;
    private static KnowledgeService knowledgeService;
    private static Knowledge knowledge;
    private static Validator XML_VALIDATOR;
    private static String DEFAULT_XML;


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
                .newRule("Initial configuration")
                .forEach(facts)
                .where("$person.flags.active == false")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        // Wakeup time in seconds
                        int wakeUpTime = World.randomGaussian(8 * 3600, 2 * 3600);
                        person.set("wakeup", wakeUpTime);
                        person.set("active", true);
                        System.out.println("!!! config " + person);
                    }
                })

                .newRule("Working persons wakeup")
                .forEach(facts)
                .where("$person.flags.active == true")
                .where("$person.flags.sleeping == true")
                .where("$person.properties.work != null")
                .where("$time.seconds > $person.numbers.wakeup")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        person
                                .set("sleeping", false)
                                .set("commuting_to_work", true)
                                .set("transit_start_time", time.absoluteTimeSeconds())
                                .set("location", null);
                        System.out.println("!!! Wakeup " + person + ", time: " + time);
                    }
                })

                .newRule("Working persons, arrival at work")
                .forEach(facts)
                .where("$person.flags.active == true")
                .where("$person.properties.work != null")
                .where("$person.flags.commuting_to_work == true")
                .where("$time.absoluteTimeSeconds - $person.numbers.transit_start_time > 1200") // 20 min
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        person
                                .set("commuting_to_work", false)
                                .set("working", true)
                                .set("location", person.getProperty("work"));

                        int currentAbsoluteTime = time.absoluteTimeSeconds();

                        int workDuration = World.randomGaussian(8 * 3600, 2 * 3600);
                        person.set("end_of_work_time", currentAbsoluteTime + workDuration);

                        System.out.println("!!! Working " + person + ", time: " + time);
                    }
                })

                .newRule("Working persons, end of work")
                .forEach(facts)
                .where("$person.flags.active == true")
                .where("$person.properties.work != null")
                .where("$person.flags.working == true")
                .where("$time.absoluteTimeSeconds > $person.numbers.end_of_work_time")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        person
                                .set("deciding_on_shopping", true)
                                .set("working", false)
                                .set("location", null);
                        System.out.println("!!! End of work, time: " + time);
                    }
                })

                .newRule("Working persons, shop or not to shop?")
                .forEach(facts)
                .where("$person.flags.active == true")
                .where("$person.flags.deciding_on_shopping == true")
                .where("$person.properties.work != null")
                .where("$person.flags.decided == false")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        person
                                .set("deciding_on_shopping", false)
                                .set("decided", true)
                                .set("post_work_shopping", World.randomBoolean(0.5));
                        System.out.println("!!! Post-work shopping: " + person.getFlag("post_work_shopping", false));
                    }
                })

                .newRule("Working persons, post-work shopping = yes")
                .forEach(facts)
                .where("$person.flags.active == true")
                .where("$person.properties.work != null")
                .where("$person.flags.decided == true")
                .where("$person.flags.in_transit == false")
                .where("$person.flags.post_work_shopping == true")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        person
                                .set("deciding_on_shopping", false)
                                .set("decided", false)
                                .set("in_transit", true);
                        //.set("post_work_shopping", World.randomBoolean(1.0));
                        System.out.println("!!! I'm shopping");
                    }
                })

                .newRule("Working persons, post-work shopping = no")
                .forEach(facts)
                .where("$person.flags.active == true")
                .where("$person.properties.work != null")
                .where("$person.flags.decided == true")
                .where("$person.flags.in_transit == false")
                .where("$person.flags.post_work_shopping == false")
                .execute(new PersonTimeConsumer() {
                    @Override
                    public void process(Entity person, WorldTime time, World world) {
                        person
                                .set("deciding_on_shopping", false)
                                .set("decided", false)
                                .set("in_transit", true);
                        //.set("post_work_shopping", World.randomBoolean(1.0));
                        System.out.println("!!! I'm heading home");
                    }
                })

                ;

    }

    public static Document buildConfigXml(String sourceXml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(sourceXml)));
            XML_VALIDATOR.validate(new DOMSource(document));
            return document;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
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


                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = factory.newSchema(ctx.getResource("/WEB-INF/schema.xsd"));
                XML_VALIDATOR = schema.newValidator();


                // Validating default
                DEFAULT_XML = Utils.readResourceAsString(ctx, "/WEB-INF/config.xml");

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