package org.evrete.helper;

import org.evrete.api.*;
import org.evrete.runtime.SessionRule;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RhsAssert implements Consumer<RhsContext>, Copyable<RhsAssert> {
    private static final Function<Rule, Entry[]> FROM_DESCRIPTOR = rule -> {
        Collection<NamedType> types = rule.getDeclaredFactTypes();
        Entry[] entries = new Entry[types.size()];
        int i = 0;
        for(NamedType t : types) {
            entries[i++] = new Entry(t.getVarName());
        }
        return entries;
    };
    private static final Function<SessionRule, Entry[]> FROM_RULE = FROM_DESCRIPTOR::apply;
    private final Map<String, Collection<Object>> collector = new HashMap<>();
    private final AtomicInteger callCounter = new AtomicInteger(0);
    private final Entry[] entries;
    private PrintStream out;


    private RhsAssert(Entry[] entries) {
        this.entries = entries;
        for (Entry entry : entries) {
            if (collector.put(entry.name, new HashSet<>()) != null) {
                throw new IllegalStateException("Duplicate entry name: " + entry.name);
            }
        }
    }

    private RhsAssert(Supplier<Entry[]> supplier) {
        this(supplier.get());
    }

    private RhsAssert(SessionRule rule) {
        this(() -> FROM_RULE.apply(rule));
        rule.chainRhs(this);
    }

    public RhsAssert(RuleSession<?> session, String name) {
        this((SessionRule) session.getRule(name));
    }

    public RhsAssert(RuleSession<?> session) {
        this(getSingleRule(session));
    }

    public RhsAssert(String var, Class<?> type) {
        this(new Entry[]{
                new Entry(var)
        });
    }

    public RhsAssert(String var1, Class<?> type1, String var2, Class<?> type2) {
        this(new Entry[]{
                new Entry(var1),
                new Entry(var2)
        });
    }

    public RhsAssert(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3) {
        this(new Entry[]{
                new Entry(var1),
                new Entry(var2),
                new Entry(var3)
        });
    }

    public RhsAssert(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4) {
        this(new Entry[]{
                new Entry(var1),
                new Entry(var2),
                new Entry(var3),
                new Entry(var4)
        });
    }

    @SuppressWarnings("unused")
    public RhsAssert(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4, String var5, Class<?> type5) {
        this(new Entry[]{
                new Entry(var1),
                new Entry(var2),
                new Entry(var3),
                new Entry(var4),
                new Entry(var5)
        });
    }

    public RhsAssert(String var1, Class<?> type1, String var2, Class<?> type2, String var3, Class<?> type3, String var4, Class<?> type4, String var5, Class<?> type5, String var6, Class<?> type6) {
        this(new Entry[]{
                new Entry(var1),
                new Entry(var2),
                new Entry(var3),
                new Entry(var4),
                new Entry(var5),
                new Entry(var6)
        });
    }

    private static SessionRule getSingleRule(RuleSession<?> s) {
        List<RuntimeRule> rules = s.getRules();
        if (rules.isEmpty()) {
            throw new IllegalStateException("Zero rule count, one expected");
        } else if (rules.size() > 1) {
            throw new IllegalStateException("Multiple rule count, one expected");
        } else {
            return (SessionRule) rules.get(0);
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
            entry.getValue().add(o);
        }

        if (out != null) {
            StringJoiner joiner = new StringJoiner(" ", ">>> ", "\t");
            values.forEach((var, o) -> joiner.add(var + "=" + o));
            out.println(joiner);
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
        assert list.contains(o) : "Expected object '" + o + "' wasn't found in the output " + list;
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

        Entry(String name) {
            this.name = name;
        }

    }
}
