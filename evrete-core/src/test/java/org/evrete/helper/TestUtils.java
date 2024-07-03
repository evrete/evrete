package org.evrete.helper;

import org.evrete.api.RuntimeContext;
import org.evrete.api.StatefulSession;
import org.evrete.api.builders.LhsBuilder;
import org.evrete.api.builders.RuleBuilder;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.classes.TypeC;
import org.evrete.classes.TypeD;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;
import java.util.StringJoiner;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public final class TestUtils {

    public static long nanoExecTime(Runnable r) {
        long t0 = System.nanoTime();
        r.run();
        return System.nanoTime() - t0;
    }

    private static <T> void deleteFrom(Collection<T> collection, Predicate<T> predicate) {
        LinkedList<T> selected = new LinkedList<>();
        for (T obj : collection) {
            if (predicate.test(obj)) selected.add(obj);
        }

        for (T o : selected) {
            collection.remove(o);
        }
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Collection<FactEntry> sessionFacts(StatefulSession s) {
        Collection<FactEntry> col = new LinkedList<>();
        s.forEachFact((handle, fact) -> col.add(new FactEntry(handle, fact)));
        return col;
    }

    public static <T> Collection<FactEntry> sessionFacts(StatefulSession s, Class<T> type) {
        Collection<FactEntry> collection = new LinkedList<>();
        s.forEachFact((handle, fact) -> {
            if (fact.getClass().equals(type)) {
                collection.add(new FactEntry(handle, fact));
            }
        });
        return collection;
    }

    public static <C extends RuntimeContext<C>> RuleSetBuilder<C> applyRandomConditions(RuleSetBuilder<C> ruleSetBuilder, String ruleName, int conditions) {
        String[] fields = new String[]{"i", "l", "d", "f"};
        Class<?>[] allClasses = new Class<?>[]{TypeA.class, TypeB.class, TypeC.class, TypeD.class};
        FieldReference1[][] references = new FieldReference1[allClasses.length][fields.length];

        RuleBuilder<C> builder = ruleSetBuilder.newRule(ruleName);

        LhsBuilder<C> rootGroup = builder.getLhs();

        for (int t = 0; t < allClasses.length; t++) {
            Class<?> cl = allClasses[t];
            String factName = cl.getSimpleName();
            rootGroup.addFactDeclaration("$" + factName, cl);
            for (int f = 0; f < fields.length; f++) {
                String fieldName = fields[f];
                references[t][f] = new FieldReference1("$" + factName, factName, fieldName);
            }
        }

        for (int c = 0; c < conditions; c++) {
            String randomCondition = randomCondition(references);
            rootGroup.where(randomCondition);
        }

        return rootGroup.execute();
    }

    private static String randomCondition(FieldReference1[][] references) {
        Random random = new Random(System.nanoTime());

        int varCount = 0;

        while (varCount > references.length || varCount < 2) {
            varCount = 1 + random.nextInt(references.length);
        }
        FieldReference1[] descriptor = new FieldReference1[varCount];
        for (int i = 0; i < varCount; i++) {
            boolean exists;
            FieldReference1 ref;
            do {
                exists = false;
                int type = random.nextInt(2048) % references.length;
                int field = random.nextInt(2048) % references[type].length;
                ref = references[type][field];
                for (int k = 0; k < i; k++) {
                    FieldReference1 test = descriptor[k];
                    if (test.factType.equals(ref.factType)) {
                        exists = true;
                        break;
                    }
                }
            } while (exists);
            descriptor[i] = ref;
        }

        StringJoiner joiner = new StringJoiner(" + ", "", " >= 0");

        for (FieldReference1 ref : descriptor) {

            String s = ref.varName + "." + ref.fieldName;
            joiner.add(s);
        }
        return joiner.toString();
    }


    static class FieldReference1 {
        private final String varName;
        private final String factType;
        private final String fieldName;

        FieldReference1(String varName, String factType, String fieldName) {
            this.varName = varName;
            this.factType = factType;
            this.fieldName = fieldName;
        }

        @Override
        public String toString() {
            return fieldName + " : " + varName;
        }

    }


}
