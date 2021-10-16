package org.evrete.examples.howto;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatelessSession;
import org.evrete.api.TypeResolver;
import org.evrete.dsl.annotation.*;

import java.io.IOException;

class CsvFactsAnnotated {
    private static final String TYPE_PERSON = "Hello Person!";
    private static final String TYPE_LOCATION = "Hello Location!";

    public static void main(String[] args) throws IOException {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service.newKnowledge();

        // Type declarations
        TypeResolver typeResolver = service.newTypeResolver();
        typeResolver.declare(TYPE_PERSON, String.class);
        typeResolver.declare(TYPE_LOCATION, String.class);


        // New 'factorial' field in a rule
        StatelessSession session = service
                .newKnowledge("JAVA-CLASS", typeResolver, StringsRuleset.class)
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

    public static class StringsRuleset {
        @Rule
        @Where(methods = {
                @MethodPredicate(method = "ageTest", args = {"$p.age"}),
                @MethodPredicate(method = "locationMatch", args = {"$p.location", "$l.street"})
        })
        public void rule1(@Fact(value = "$p", type = TYPE_PERSON) String person, @Fact(value = "$l", type = TYPE_LOCATION) String location) {
            System.out.println("Match: <" + person + "> at <" + location + ">");
        }

        @FieldDeclaration(type = TYPE_PERSON, name = "age")
        public int method1(String s) {
            return Integer.parseInt(s.split(",")[1]);
        }

        @FieldDeclaration(type = TYPE_PERSON, name = "location")
        public String method2(String s) {
            return s.split(",")[2];
        }

        @FieldDeclaration(type = TYPE_LOCATION, name = "street")
        public String method3(String s) {
            return s.split(",")[0];
        }

        @FieldDeclaration(type = TYPE_LOCATION, name = "zip")
        public int method4(String s) {
            return Integer.parseInt(s.split(",")[1]);
        }

        public boolean ageTest(int age) {
            return age > 18;
        }

        public boolean locationMatch(String s1, String s2) {
            return s1.equals(s2);
        }

    }
}