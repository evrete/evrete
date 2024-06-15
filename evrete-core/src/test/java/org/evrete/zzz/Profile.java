package org.evrete.zzz;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

//TODO delete me!!!!
public class Profile {

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge eKnowledge = service.newKnowledge()
                .builder()
                .newRule("sales")
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
                })
                .build();
        withKnowledge(eKnowledge);

        service.shutdown();
    }

    private static void withKnowledge(Knowledge knowledge) {

        int counter = 0;
        long sumInsertTime = 0;
        long sumFireTime = 0;
        for (int i = 0; i < 100_000; i++) {

            SalesReport report = new SalesReport();
            try(StatefulSession s = knowledge.newStatefulSession()) {
                long t0 = Instant.now().toEpochMilli();
                List<Object> objects = sessionObjects(512);
                s.insert(objects);
                s.insert(report);
                long t1 = Instant.now().toEpochMilli();
                s.fire();
                long t2 = Instant.now().toEpochMilli();
                long insertTime = t1 - t0;
                long fireTime = t2 - t1;
                sumInsertTime += insertTime;
                sumFireTime += fireTime;
                counter++;
            }

            if(counter == 100) {
                System.out.println("Insert : " + (sumInsertTime/counter) + "\tFire: " + (sumFireTime/counter) + " : " + report);
                sumInsertTime = 0;
                sumFireTime = 0;
                counter = 0;
            }
        }
    }

    private static List<Object> sessionObjects(int scale) {
        List<Object> sessionObjects = new ArrayList<>();
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

        return sessionObjects;
    }

}
