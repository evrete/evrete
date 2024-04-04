package org.evrete.examples.howto;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatelessSession;
import org.evrete.api.Type;

public class CsvFactsInline {
    private static final String TYPE_PERSON = "Person Type";
    private static final String TYPE_LOCATION = "Location Type";

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service.newKnowledge()
                .configureTypes(resolver -> {
                    // Person type & fields
                    Type<String> pt = resolver.declare(TYPE_PERSON, String.class);
                    pt.declareField("name", String.class, s -> s.split(",")[0]);
                    pt.declareIntField("age", s -> Integer.parseInt(s.split(",")[1]));
                    pt.declareField("location", String.class, s -> s.split(",")[2]);

                    // Location type & fields
                    Type<String> lt = resolver.declare(TYPE_LOCATION, String.class);
                    lt.declareField("address", String.class, s -> s.split(",")[0]);
                    lt.declareIntField("zip", s -> Integer.parseInt(s.split(",")[1]));
                });

        // Build knowledge & session
        StatelessSession session = knowledge
                .builder()
                .newRule()
                .forEach(
                        "$p", TYPE_PERSON,
                        "$l", TYPE_LOCATION)
                .where("$p.location.equals($l.address)")
                .where("$p.age > 18")
                .execute(ctx -> {
                    String person = ctx.get("$p");
                    String location = ctx.get("$l");
                    System.out.println("Match: <" + person + "> at <" + location + ">");
                })
                .build()
                .newStatelessSession();

        // Mike is 17 y.o., located at '5246 Elm Street'
        String p1 = "Mike,17,5246 Elm Street";
        // Janet is 45 y.o., same location
        String p2 = "Janet,45,5246 Elm Street";
        // Location '5246 Elm Street', ZIP 123456
        String loc1 = "5246 Elm Street,123456";
        // Location '143 Rose Avenue', ZIP 345678
        String loc2 = "143 Rose Avenue,345678";

        session.insertAs(TYPE_PERSON, p1, p2);
        session.insertAs(TYPE_LOCATION, loc1, loc2);
        session.fire();

        service.shutdown();
    }
}
