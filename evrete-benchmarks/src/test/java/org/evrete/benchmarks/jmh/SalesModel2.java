package org.evrete.benchmarks.jmh;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.benchmarks.helper.SessionWrapper;
import org.evrete.benchmarks.helper.TestUtils;
import org.evrete.benchmarks.models.sales.Customer;
import org.evrete.benchmarks.models.sales.Department;
import org.evrete.benchmarks.models.sales.Invoice;
import org.evrete.benchmarks.models.sales.SalesReport;
import org.kie.api.runtime.KieContainer;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("MethodMayBeStatic")
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 1)
public class SalesModel2 {

    @Benchmark
    public void drools(BenchmarkState state) {
        SessionWrapper s = state.droolsSession();
        for (Invoice i : state.invoices) {
            s.insert(i);
        }
        s.insert(state.reportScope);
        s.insert(state.report);
        s.fire();
        s.close();
    }


    @Benchmark
    public void evrete(BenchmarkState state) {
        SessionWrapper s = state.evreteSession();
        for (Invoice i : state.invoices) {
            s.insert(i);
        }
        s.insert(state.reportScope);
        s.insert(state.report);
        s.fire();
        s.close();
    }

    @SuppressWarnings("unused")
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        //static final int totalInvoices = 1<<16;
        private final Random random = new Random();
        List<Invoice> invoices;
        Department reportScope;
        SalesReport report;
        @Param({"10", "100", "1000", "10000", "100000"})
        int totalInvoices;
        private KnowledgeService service;
        private KieContainer dKnowledge;
        private Knowledge eKnowledge;

        @Setup(Level.Invocation)
        public void initInvocationData() {
            List<Department> departments = new ArrayList<>();
            for (int d = 0; d < 8; d++) {
                departments.add(new Department(d));
            }
            List<Customer> customers = new ArrayList<>();
            invoices = new ArrayList<>();
            for (int i = 0; i < totalInvoices; i++) {
                Department d = departments.get(i % departments.size());
                Invoice invoice = new Invoice(i + 0.0, null, d);
                invoices.add(invoice);
            }

            report = new SalesReport();
            reportScope = departments.get(random.nextInt(departments.size()));
        }

        SessionWrapper droolsSession() {
            return SessionWrapper.of(dKnowledge.newKieSession());
        }

        SessionWrapper evreteSession() {
            return SessionWrapper.of(eKnowledge.createSession());
        }

        @Setup(Level.Trial)
        public void initKnowledge() {
            service = new KnowledgeService();
            eKnowledge = service.newKnowledge();
            eKnowledge.newRule("sample01")
                    .forEach(
                            "$report", SalesReport.class,
                            "$i", Invoice.class,
                            "$d", Department.class
                    )
                    //.where("$i.customer.id == $c.id", 1.0)
                    .where("$d.id == $i.department.id")
                    .execute(ctx -> {
                        SalesReport report = ctx.get("$report");
                        Invoice i = ctx.get("$i");
                        report.sales += i.amount;
                    });

            // Drools
            dKnowledge = TestUtils.droolsKnowledge("src/test/drl/sales2.drl");
        }

        @TearDown(Level.Trial)
        public void destroyKnowledge() {
            service.shutdown();
            dKnowledge.dispose();
        }
    }
}
