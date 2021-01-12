package org.evrete.showcase.abs.town;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RhsContext;
import org.evrete.api.Type;
import org.evrete.api.TypeResolver;
import org.evrete.showcase.abs.town.json.GeoData;
import org.evrete.showcase.abs.town.types.Entity;
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
    static KnowledgeService knowledgeService;
    static Knowledge knowledge;
    static GeoData MAP_DATA;
//    static Validator XML_VALIDATOR;
//    static String DEFAULT_XML;


    static KnowledgeService knowledgeService() {
        if (knowledgeService == null) {
            throw new IllegalStateException();
        } else {
            return knowledgeService;
        }
    }

    public static Knowledge knowledge() {
        return knowledge;
    }

    static Knowledge buildKnowledge() {
        Knowledge knowledge = knowledgeService().newKnowledge();
        TypeResolver defaultResolver = knowledge.getTypeResolver();
        Type<Entity> entityType = defaultResolver.declare(Entity.class);
        defaultResolver.wrapType(new Entity.EntityKnowledgeType(entityType));

        return knowledge
                .newRule("Initial configuration")
                .forEach(
                        "$person", Entity.class,
                        "$world", World.class
                )
                .where("eq($person.type, 'person')")
                .where("$person.flags.active == false")
                .execute(new Consumer<RhsContext>() {
                    @Override
                    public void accept(RhsContext ctx) {
                        Entity $person = ctx.get("$person");
                        World world = ctx.get("$world");


                        // Wakeup time in seconds
                        int wakeUpTime = world.randomGaussian(6 * 3600, 3600);
                        //$person.set("wakeup", wakeUpTime);

                        // Random sleep time
                        int sleepDuration = world.randomGaussian(6 * 3600, 2 * 3600);

                        // Bedtime
                        int bedTime = wakeUpTime - sleepDuration;
                        while (bedTime < 0) {
                            bedTime = bedTime + 24 * 3600;
                        }

                        //$person.set("bedtime", bedTime);
                        $person.set("active", true);
                        System.out.println("!!! config " + $person);

                        ctx.update($person);
                    }
                })
                .newRule("Working persons wakeup")
                .forEach(
                        "$person", Entity.class,
                        "$time", WorldTime.class
                )
                .where("$person.flags.active == true")
                .where("$person.properties.work != null")
                .where("$person.properties.location == $person.properties.home")
                .where("$person.flags.transit == false")
                .where("$time.seconds > $person.numbers.wakeup")
                .execute(new Consumer<RhsContext>() {
                    @Override
                    public void accept(RhsContext ctx) {
                        Entity $person = ctx.get("$person");
                        WorldTime time = ctx.get("$time");
                        $person.set("transit", true);
                        $person.set("transit_start_time", time.absoluteTimeSeconds());
                        $person.set("location", null);
                        ctx.update($person);
                        System.out.println("!!! Wakeup " + $person);
                    }
                })

                .newRule("Working persons, arrival at work")
                .forEach(
                        "$person", Entity.class,
                        "$time", WorldTime.class
                )
                .where("$person.flags.active == true")
                .where("$person.properties.work != null")
                //.where("$person.properties.location == null")
                .where("$person.flags.transit == true")
                //.where("$time.absoluteTimeSeconds - $person.numbers.transit_start_time > 1200") // 20 min
                .execute(new Consumer<RhsContext>() {
                    @Override
                    public void accept(RhsContext ctx) {

                        Entity $person = ctx.get("$person");
                        if (!$person.getFlag("transit", false)) {
                            System.err.println("ERR\n" + $person);
                            throw new IllegalStateException();
                        }
                        Entity loc = $person.getProperty("location");
                        System.out.println("Arrived " + $person);
                        $person.set("transit", false);
                        $person.set("location", $person.getProperty("work"));
                        ctx.update($person);
                    }
                });

    }

/*
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
*/

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


/*
                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = factory.newSchema(ctx.getResource("/WEB-INF/schema.xsd"));
                XML_VALIDATOR = schema.newValidator();
*/


                // Validating default
/*
                DEFAULT_XML = Utils.readResourceAsString(ctx, "/WEB-INF/config.xml");
                new TransitionManager(DEFAULT_XML, 3600); // Expect no exceptions are thrown
*/

/*
                TransitionManager tm =  new TransitionManager(DEFAULT_XML, 15); // Expect no exceptions are thrown
                System.out.println("TM: " + tm);
                int count = 100_000;
                EnumMap<State, Integer> map = new EnumMap<>(State.class);
                for(int i=0; i< count; i++) {
                    State newState = tm.newRandomState(State.HOME, 5);
                    Integer cnt = map.get(newState);
                    if(cnt == null){
                        cnt = 0;
                    }
                    map.put(newState, cnt + 1);

                    if(newState != State.HOME) {
                        //System.out.println(i + " : " + newState);
                    }
                }
                System.out.println(map);
*/

                //buildConfigXml(DEFAULT_XML);

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
}