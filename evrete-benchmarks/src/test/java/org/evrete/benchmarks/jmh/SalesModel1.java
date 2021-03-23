package org.evrete.benchmarks.jmh;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.benchmarks.helper.SessionWrapper;
import org.evrete.benchmarks.helper.TestUtils;
import org.evrete.benchmarks.models.sales.Customer;
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
public class SalesModel1 {

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

/*
    @Benchmark
    public void baseline(BenchmarkState state) {
        SalesReport report = state.report;
        for (Invoice i : state.invoices) {
            if (i.customer.id == state.reportScope.id) {
                report.sales += i.amount;
            }
        }
    }
*/

    @SuppressWarnings("unused")
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        //static final int totalInvoices = 1<<16;
        private final Random random = new Random();
        List<Invoice> invoices;
        List<Customer> customers;
        Customer reportScope;
        SalesReport report;
        @Param({"16"})
        int customerBase;
        @Param({"256", "512", "1024", "2048", "4096", "8192", "16384"})
        int totalInvoices;
        private KnowledgeService service;
        private KieContainer dKnowledge;
        private Knowledge eKnowledge;

        @Setup(Level.Invocation)
        public void initInvocationData() {
            customers = new ArrayList<>();
            invoices = new ArrayList<>();
            for (int c = 0; c < customerBase; c++) {
                Customer customer = new Customer(c, "long customer prefix which affects the equals operation " + c);
                customers.add(customer);
                int invoicesPerCustomer = totalInvoices / customerBase;
                //int invoicesPerCustomer = 2;
                for (int i = 0; i < invoicesPerCustomer; i++) {
                    Invoice invoice = new Invoice(i + 0.0, customer);
                    invoices.add(invoice);
                }
            }
            report = new SalesReport();
            reportScope = customers.get(random.nextInt(customers.size()));
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
                            "$c", Customer.class
                    )
                    //.where("$i.customer.id == $c.id", 1.0)
                    .where("$i.customer.sameName($c.name)")
                    .execute(ctx -> {
                        SalesReport report = ctx.get("$report");
                        Invoice i = ctx.get("$i");
                        report.sales += i.amount;
                    });

            // Drools
            dKnowledge = TestUtils.droolsKnowledge("src/test/drl/sales1.drl");
        }

        @TearDown(Level.Trial)
        public void destroyKnowledge() {
            service.shutdown();
            dKnowledge.dispose();
        }
    }
}
