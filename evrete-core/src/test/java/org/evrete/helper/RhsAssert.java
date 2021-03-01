package org.evrete.helper;

import org.evrete.api.Copyable;
import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.runtime.FactType;
import org.evrete.runtime.RuleDescriptor;
import org.evrete.runtime.RuntimeRuleImpl;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RhsAssert implements Consumer<RhsContext>, Copyable<RhsAssert> {
    private static final Function<RuleDescriptor, Entry[]> FROM_DESCRIPTOR = rule -> {
        FactType[] types = rule.getFactTypes();
        Entry[] entries = new Entry[types.length];
        int i = 0;
        for (FactType t : types) {
            entries[i++] = new Entry(t.getVar(), t.getType().getName());
        }
        return entries;
    };
    private static final Function<RuntimeRuleImpl, Entry[]> FROM_RULE = rule -> FROM_DESCRIPTOR.apply(rule.getDescriptor());
    private final Map<String, Collection<Object>> collector = new HashMap<>();
    private final Map<String, String> types = new HashMap<>();
    private final AtomicInteger callCounter = new AtomicInteger(0);
    private final Entry[] entries;
    private PrintStream out;


    private RhsAssert(Entry[] entries) {
        this.entries = entries;
        for (Entry entry : entries) {
            if (collector.put(entry.name, new HashSet<>()) != null) {
                throw new IllegalStateException("Duplicate entry name: " + entry.name);
            }
            types.put(entry.name, entry.clazz);
        }
    }

    private RhsAssert(Supplier<Entry[]> supplier) {
        this(supplier.get());
    }

    private RhsAssert(RuntimeRuleImpl rule) {
        this(() -> FROM_RULE.apply(rule));
        rule.chainRhs(this);
    }

    public RhsAssert(StatefulSession statefulSession, String name) {
        this((RuntimeRuleImpl) statefulSession.getRule(name));
    }

    public RhsAssert(StatefulSession statefulSession) {
        this(getSingleRule(statefulSession));
    }

    public RhsAssert(String var, Class<?> type) {
        this(new Entry[]{
                new Entry(var, type)
        });
    }

    public RhsAssert(String var1, Class<?> type1, String var2, Class<?> type2) {
        this(new Entry[]{
                new Entry(var1, type1),
                new Entry(var2, type2)
        });
    }

    public RhsAssert(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3) {
        this(new Entry[]{
                new Entry(var1, type1),
                new Entry(var2, type2),
                new Entry(var3, type3)
        });
    }

    public RhsAssert(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4) {
        this(new Entry[]{
                new Entry(var1, type1),
                new Entry(var2, type2),
                new Entry(var3, type3),
                new Entry(var4, type4)
        });
    }

    @SuppressWarnings("unused")
    public RhsAssert(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4, String var5, Class<?> type5) {
        this(new Entry[]{
                new Entry(var1, type1),
                new Entry(var2, type2),
                new Entry(var3, type3),
                new Entry(var4, type4),
                new Entry(var5, type5)
        });
    }

    public RhsAssert(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4, String var5, Class<?> type5, String var6, Class<?> type6) {
        this(new Entry[]{
                new Entry(var1, type1),
                new Entry(var2, type2),
                new Entry(var3, type3),
                new Entry(var4, type4),
                new Entry(var5, type5),
                new Entry(var6, type6)
        });
    }

    private static RuntimeRuleImpl getSingleRule(StatefulSession s) {
        List<RuntimeRule> rules = s.getRules();
        if (rules.size() == 0) {
            throw new IllegalStateException("Zero rule count, one expected");
        } else if (rules.size() > 1) {
            throw new IllegalStateException("Multiple rule count, one expected");
        } else {
            return (RuntimeRuleImpl) rules.get(0);
        }
    }

    @Override
    public RhsAssert copyOf() {
        return new RhsAssert(Arrays.copyOf(this.entries, this.entries.length));
    }

    public void reset() {
        callCounter.set(0);
        this.out = null;
        for (Collection<Object> objects : collector.values()) {
            objects.clear();
        }
    }

    @Override
    public void accept(RhsContext ctx) {
        callCounter.incrementAndGet();
        Map<String, Object> values = new HashMap<>();
        for (Map.Entry<String, Collection<Object>> entry : collector.entrySet()) {
            String var = entry.getKey();

            Object o = ctx.get(var);
            values.put(var, o);
            Class<?> cl = o.getClass();
            String expected = types.get(var);
            if (expected == null) throw new IllegalStateException("Unknown type");

            if (!cl.getName().equals(expected))
                throw new IllegalStateException("Type mismatch for '" + var + "'. Expected type: '" + expected + "', Found: " + cl.getName());

            entry.getValue().add(o);
        }

        if (out != null) {
            StringJoiner joiner = new StringJoiner(" ", ">>> ", "\t");
            values.forEach((var, o) -> joiner.add(var + "=" + o));
            out.println(joiner.toString());
        }

    }

    public RhsAssert assertCount(int total) {
        assert callCounter.get() == total : "Actual " + callCounter.get() + " vs expected " + total;
        return this;
    }

    public int getCount() {
        return callCounter.get();
    }

    @SuppressWarnings("unused")
    public void debugOut(PrintStream stream) {
        this.out = stream;
    }

    @SuppressWarnings("unused")
    public void debugCounts(PrintStream stream) {
        Map<String, Integer> map = new HashMap<>();
        collector.forEach((s, objects) -> map.put(s, objects.size()));

        stream.println(map);
    }

    public RhsAssert assertUniqueCount(String var, int count) {
        Collection<Object> list = collector.get(var);
        if (list == null) throw new IllegalStateException("Unknown var '" + var + "'");
        assert list.size() == count : "Actual size for '" + var + "' is " + list.size() + ", expected value: " + count;
        return this;
    }

    public RhsAssert assertContains(String var, Object o) {
        Collection<Object> list = collector.get(var);
        if (list == null) throw new IllegalStateException("Unknown var '" + var + "'");
        assert list.contains(o);
        return this;
    }

    public RhsAssert assertNotContains(String var, Object o) {
        Collection<Object> list = collector.get(var);
        if (list == null) throw new IllegalStateException("Unknown var '" + var + "'");
        assert !list.contains(o);
        return this;
    }

    private static class Entry {
        private final String name;
        private final String clazz;

        Entry(String name, String clazz) {
            this.name = name;
            this.clazz = clazz;
        }

        Entry(String name, Class<?> clazz) {
            this(name, clazz.getName());
        }
    }
}
