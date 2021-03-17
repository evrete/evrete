/*
 * J A V A  C O M M U N I T Y  P R O C E S S
 *
 * J S R  9 4
 *
 * Test Compatibility Kit
 *
 */
package org.jcp.jsr94.tck;

// java imports

import org.jcp.jsr94.tck.model.Customer;
import org.jcp.jsr94.tck.model.Invoice;
import org.jcp.jsr94.tck.util.TestCaseUtil;
import org.jcp.jsr94.tck.util.TestObjectFilter;
import org.junit.jupiter.api.Test;

import javax.rules.ObjectFilter;
import javax.rules.StatelessRuleSession;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Test the javax.rules.StatelessRuleSession class.
 * <p>
 * The definition of the rule execution set can be found in the
 * "tck_res_1.xml" file.<br>
 * This RuleExecutionSet will be invoked by the TCK in a stateless
 * manner.<br>
 * The rule execution set must have support for the following business
 * object model:
 * <ul>
 * <li>Customer Class.<br>
 * The Customer business object is a simple business object that
 * contains a name and credit limit property. The definition of this
 * class can be found in {@link org.jcp.jsr94.tck.model.Customer}
 * <li>Invoice Class.<br>
 * The Invoice business object is a simple business object that
 * contains a description, amount, and status property. The definition
 * of this class can be found in {@link org.jcp.jsr94.tck.model.Invoice}
 * </ul>
 * <p>
 * The rule execution set has the following definition:
 * <ul>
 * <li>Support Customer and Invoice business objects.
 * <li>Defines 1 logical rule.<br>
 * Rule1:<br>
 * If the credit limit of the customer is greater than the amount of
 * the  invoice and the status of the invoice is unpaid then
 * decrement the credit limit with the amount of the invoice and
 * set the status of the invoice to "paid".
 * </ul>
 * <p>
 * <b>Note:</b><br>
 * Additional physical rules may be defined to accomplish the
 * requirements mentioned above.<p>
 * The rule execution set has the following semantics:
 * <ul>
 * <li>Input: <br>
 * A Customer with a credit limit of 5000.<br>
 * An Invoice with an amount of 2000.<br>
 * </ul>
 * The rule execution should produce the following output:
 * <ul>
 * <li>The credit limit of the customer is 3000
 * <li>The status of the invoice is paid.
 * </ul>
 * <b>Performs the following tests:</b><br>
 * <ul>
 * <li>Basic API tests. {@link #testStatelessRuleSession}
 * <ul>
 * <li>Create stateless rule session
 * <li>Execute Rules and verify results
 * <li>Execute Rules with object filtering
 * </ul>
 * </ul>
 *
 * @version 1.0
 * @see javax.rules.StatelessRuleSession
 * @since JSR-94 1.0
 */
class StatelessRuleSessionTest {
    /**
     * Test the compliance for javax.rules.StatelessRuleSession.
     * Create a stateless rule session and execute the rules on a
     * Customer and Invoice object. The execution of the rule will be
     * invoked with an without an object filter. The object filter is
     * the TestObjectFilter of the TCK.
     *
     * <p>
     * <b>Description:</b><br>
     * <ul>
     * <li>Create stateless rule session
     * <ul>
     * <li>Fail: If any exception occurs while creating the stateless
     * rule session
     * <li>Succeed: If the stateless rule session can successfully be
     * created.
     * </ul>
     * <li>Execute Rules and verify results
     * <ul>
     * <li>Fail: If any error occurs during execution of the rules or
     * if the results do not match the expected output.
     * <li>Succeed: If the execute rules produced the expected output.
     * </ul>
     * <li>Execute Rules with object filtering
     * <ul>
     * <li>Fail: If any error occurs during the execution of the rules
     * or if the results to not match the expected output.
     * <li>Succeed: If the execution of the rules produced the correct
     * set of output objects after filtering.
     * </ul>
     * </ul>
     *
     * @see TestCaseUtil#getStatelessRuleSession
     * @see TestObjectFilter
     * @see Customer
     * @see Invoice
     */
    @Test
    void testStatelessRuleSession() {
        try {
            StatelessRuleSession ruleSession = TestCaseUtil.getStatelessRuleSession("stateless", "src/test/resources/TckRes1.java");
            assert ruleSession != null;

            // Create a Customer as specified by the TCK documentation.
            Customer inputCustomer = new Customer("test");
            inputCustomer.setCreditLimit(5000);

            // Create an Invoice as specified by the TCK documentation.
            Invoice inputInvoice = new Invoice("test");
            inputInvoice.setAmount(2000);

            // Create a input list.
            List<Object> input = new ArrayList<>();
            input.add(inputCustomer);
            input.add(inputInvoice);

            // Execute the rules without a filter.
            List<?> results = ruleSession.executeRules(input);

            // Check the results.
            assert results != null;
            assert results.size() == 2;

            Iterator<?> itr = results.iterator();

            Customer resultCustomer = null;
            Invoice resultInvoice = null;

            while (itr.hasNext()) {
                Object obj = itr.next();
                if (obj instanceof Customer)
                    resultCustomer = (Customer) obj;
                if (obj instanceof Invoice)
                    resultInvoice = (Invoice) obj;
            }

            // We should have a customer and an invoice.
            assert resultCustomer != null;
            assert resultInvoice != null;

            // Verify the results (although technically speaking we
            // shouldn't verify of the rule engine works.
            assert resultCustomer.getCreditLimit() == 3000;
            assert resultInvoice.getStatus().equals("paid");


            // ================= With ObjectFilter for Customer =======
            // Create a Customer as specified by the TCK documentation.
            inputCustomer = new Customer("test");
            inputCustomer.setCreditLimit(5000);

            // Create an Invoice as specified by the TCK documentation.
            inputInvoice = new Invoice("test");
            inputInvoice.setAmount(2000);

            // Create a input list.
            input = new ArrayList<>();
            input.add(inputCustomer);
            input.add(inputInvoice);

            // Create the object filter.
            ObjectFilter customerFilter = new TestObjectFilter(TestObjectFilter.CUSTOMER_FILTER);
            // Execute the rules without a filter.
            results = ruleSession.executeRules(input, customerFilter);

            // Check the results.
            assert results != null;
            // We should only have the customer.
            assert results.size() == 1 : "Actual: " + results.size();

            itr = results.iterator();

            resultCustomer = null;
            resultInvoice = null;

            while (itr.hasNext()) {
                Object obj = itr.next();
                if (obj instanceof Customer)
                    resultCustomer = (Customer) obj;
                if (obj instanceof Invoice)
                    resultInvoice = (Invoice) obj;
            }

            // We should only have a customer.
            assert resultCustomer != null;
            assert resultInvoice == null;

            // Release the session.
            ruleSession.release();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
