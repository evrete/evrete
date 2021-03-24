package some.test.pkg;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;
import org.jcp.jsr94.tck.model.Customer;
import org.jcp.jsr94.tck.model.Invoice;

public class TckRes2 {
    /*
    If the credit limit of the customer is greater than the amount of the
    invoice and the status of the invoice is unpaid then
    decrement the credit limit with the amount of the invoice and
    set the status of the invoice to "paid".
 */
    @Where(value = {
            "$customer.creditLimit > $invoice.amount",
            "$invoice.status.equals(\"unpaid\")"
    })
    @Rule("Rule1")
    public void rule(RhsContext ctx, @Fact("$customer") Customer customer, @Fact("$invoice") Invoice invoice) {
        // Decreasing credit limit
        customer.setCreditLimit(customer.getCreditLimit() - invoice.getAmount());

        // Marking the invoice as paid
        invoice.setStatus("paid");

        ctx.updateFact("$invoice");
        ctx.updateFact("$customer");
    }

}