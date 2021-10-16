package org.evrete.examples.howto;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatelessSession;
import org.evrete.api.Type;
import org.evrete.api.TypeResolver;

class CsvFactsInline {
    private static final String TYPE_PERSON = "Hello Person!";
    private static final String TYPE_LOCATION = "Hello Location!";

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service.newKnowledge();

        // Type declarations
        TypeResolver typeResolver = knowledge.getTypeResolver();
        Type<String> personType = typeResolver.declare(TYPE_PERSON, String.class);
        Type<String> locationType = typeResolver.declare(TYPE_LOCATION, String.class);

        // Person fields
        personType.declareField("name", String.class, s -> s.split(",")[0]);
        personType.declareIntField("age", s -> Integer.parseInt(s.split(",")[1]));
        personType.declareField("location", String.class, s -> s.split(",")[2]);

        // Location fields
        locationType.declareField("street", String.class, s -> s.split(",")[0]);
        locationType.declareIntField("zip", s -> Integer.parseInt(s.split(",")[1]));

        // New 'factorial' field in a rule
        StatelessSession session = knowledge
                .newRule()
                .forEach(
                        "$person", TYPE_PERSON,
                        "$location", TYPE_LOCATION)
                .where("$person.location.equals($location.street)")
                .where("$person.age > 18")
                .execute(ctx -> {
                    String person = ctx.get("$person");
                    String location = ctx.get("$location");
                    System.out.println("Match: <" + person + "> at <" + location + ">");
                })
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