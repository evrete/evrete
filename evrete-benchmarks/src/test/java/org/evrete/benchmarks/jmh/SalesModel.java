package org.evrete.benchmarks.jmh;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.benchmarks.helper.SessionWrapper;
import org.evrete.benchmarks.helper.TestUtils;
import org.evrete.benchmarks.models.sales.Customer;
import org.evrete.benchmarks.models.sales.Invoice;
import org.evrete.benchmarks.models.sales.SalesReport;
import org.evrete.benchmarks.models.sales.SalesUnit;
import org.kie.api.runtime.KieContainer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("MethodMayBeStatic")
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 1)
public class SalesModel {

    @Benchmark
    public void drools(BenchmarkState state) {
        benchmark(state, state.droolsSession);
    }

    @Benchmark
    public void evrete(BenchmarkState state) {
        benchmark(state, state.evreteSession);
    }

    private void benchmark(BenchmarkState state, SessionWrapper s) {
        for (Object o : state.sessionObjects) {
            s.insert(o);
        }
        s.insert(new SalesReport());
        s.fire();
        s.close();
    }

    @Benchmark
    public void baseline(BenchmarkState state) {
        SalesReport report = new SalesReport();
        for(Object o1 : state.sessionObjects) {
            if(o1 instanceof SalesUnit) {
                SalesUnit u = (SalesUnit) o1;
                for(Object o2 : state.sessionObjects) {
                    if(o2 instanceof Customer) {
                        Customer c = (Customer) o2;
                        if (c.rating > 4.0) {
                            for (Object o3 : state.sessionObjects) {
                                if (o3 instanceof Invoice) {
                                    Invoice i = (Invoice) o3;
                                    if (i.salesUnit == u && i.customer.id == c.id) {
                                        report.add(u, i.amount);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        int b = report.hashCode() > 1000? 1:2;
        Blackhole.consumeCPU(b);
    }


    @SuppressWarnings("unused")
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        private final Random random = new Random();
        List<Object> sessionObjects;
        @Param({"1", "2", "4", "8", "16", "32", "64", "128", "256", "512"})
        int scale;
        SessionWrapper droolsSession;
        SessionWrapper evreteSession;
        private KnowledgeService service;
        private KieContainer dKnowledge;
        private Knowledge eKnowledge;

        @Setup(Level.Iteration)
        public void initInvocationData() {
            sessionObjects = new ArrayList<>();
            List<SalesUnit> units = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                SalesUnit unit = new SalesUnit(i);
                units.add(unit);
                sessionObjects.add(unit);
            }

            int customerId = 0;
            for (int s = 0; s < scale; s++) {
                for (int c = 0; c < 2; c++) {
                    Customer customer = new Customer(customerId++);
                    sessionObjects.add(customer);
                    for (int i = 0; i < 24; i++) {
                        SalesUnit unit = units.get(i % units.size());
                        Invoice invoice = new Invoice(i + 0.0, customer, unit);
                        sessionObjects.add(invoice);
                    }
                }
            }

        }

        @Setup(Level.Invocation)
        public void initSessions() {
            droolsSession = SessionWrapper.of(dKnowledge.newKieSession());
            evreteSession = SessionWrapper.of(eKnowledge.createSession());
            Collections.shuffle(sessionObjects);
        }

        @Setup(Level.Trial)
        public void initKnowledge() {
            service = new KnowledgeService();
            eKnowledge = service.newKnowledge();
            eKnowledge.newRule("sales")
                    .forEach(
                            "$report", SalesReport.class,
                            "$unit", SalesUnit.class,
                            "$i", Invoice.class,
                            "$c", Customer.class
                    )
                    .where("$i.salesUnit == $unit")
                    .where("$i.customer.id == $c.id")
                    .where("$c.rating > 4.0")
                    .execute(ctx -> {
                        SalesReport report = ctx.get("$report");
                        Invoice i = ctx.get("$i");
                        SalesUnit unit = ctx.get("$unit");
                        report.add(unit, i.amount);
                    });

            // Drools
            dKnowledge = TestUtils.droolsKnowledge("src/test/drl/sales-model.drl");
        }

        @TearDown(Level.Trial)
        public void destroyKnowledge() {
            service.shutdown();
            dKnowledge.dispose();
        }
    }
}
