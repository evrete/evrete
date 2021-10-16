package org.evrete.examples.howto;

import org.evrete.KnowledgeService;
import org.evrete.api.StatelessSession;
import org.evrete.api.TypeResolver;
import org.evrete.dsl.annotation.*;

public class CsvFactsAnnotated {
    private static final String TYPE_PERSON = "Person Type";
    private static final String TYPE_LOCATION = "Location Type";

    public static void main(String[] args) throws Exception {
        KnowledgeService service = new KnowledgeService();

        // Type declarations
        TypeResolver resolver = service.newTypeResolver();
        resolver.declare(TYPE_PERSON, String.class);
        resolver.declare(TYPE_LOCATION, String.class);

        // Build knowledge & session
        StatelessSession session = service
                .newKnowledge("JAVA-CLASS", resolver, CvsTypesRuleset.class)
                .newStatelessSession();

        // Mike is 16 y.o., located at '5246 Elm Street'
        String p1 = "Mike,16,5246 Elm Street";
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

    public static class CvsTypesRuleset {
        @Rule
        @Where(methods = {
            @MethodPredicate(method = "test1", args = {"$p.age"}),
            @MethodPredicate(method = "test2", args = {"$p.location", "$l.address"})
        })
        public void rule1(@Fact(value = "$p", type = TYPE_PERSON) String p, @Fact(value = "$l", type = TYPE_LOCATION) String l) {
            System.out.println("Match: <" + p + "> at <" + l + ">");
        }

        @FieldDeclaration(type = TYPE_PERSON, name = "age")
        public int field1(String s) {
            return Integer.parseInt(s.split(",")[1]);
        }

        @FieldDeclaration(type = TYPE_PERSON, name = "location")
        public String field2(String s) {
            return s.split(",")[2];
        }

        @FieldDeclaration(type = TYPE_LOCATION, name = "address")
        public String field3(String s) {
            return s.split(",")[0];
        }

        @FieldDeclaration(type = TYPE_LOCATION, name = "zip")
        public int field4(String s) {
            return Integer.parseInt(s.split(",")[1]);
        }

        public boolean test1(int age) {
            return age > 18;
        }

        public boolean test2(String s1, String s2) {
            return s1.equals(s2);
        }
    }
}