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

import javax.rules.Handle;
import javax.rules.ObjectFilter;
import javax.rules.StatefulRuleSession;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Test the javax.rules.StatefulRuleSession class.
 * <p>
 * The definition of this rule execution set can be found in the
 * "tck_res_2.xml" file.<br>
 * This RuleExecutionSet will be invoked by the TCK in a stateful manner.<br>
 * This rule execution set will first be invoked with one set of parameters,
 * process this input and keep the state of the execution.
 * A subsequent invocation will add additional information to the
 * rule execution set and the processing will involve both the newly
 * provided information as well as the processed information of the
 * previous execution.
 * <p>
 * The rule execution set must have support for the following business
 * object model:
 * <ul>
 * <li>Customer Class.<br>
 * The Customer business object is a simple business object that
 * contains  a name and credit limit property. The definition of this
 * class  can be found in {@link org.jcp.jsr94.tck.model.Customer}
 * <li>Invoice Class.<br>
 * The Invoice business object is a simple business object that
 * contains a  description, amount, and status property.
 * The definition of this class can be found
 * in {@link org.jcp.jsr94.tck.model.Invoice}
 * </ul>
 * The rule execution set has the following definition:
 * <ul>
 * <li>Support Invoice and Customer business object as input.
 * <li>Defines 1 logical rule.<br>
 * Rule1:<br>
 * If the credit limit of the customer is greater than the amount of
 * the invoice and the status of the invoice is unpaid then
 * decrement the credit limit with the amount of the invoice and
 * set the status of the invoice to "paid".
 * </ul>
 * <p>
 * <b>Note:</b><br>
 * Additional physical rules may be defined to accomplish the
 * requirements mentioned above.
 * <p>
 * The rule execution set has the following semantics:<br>
 * The first input to the rule execution set is:
 * <ul>
 * <li>A Customer with a credit limit of 5000.
 * <li>An Invoice with an amount of 2000.
 * </ul>
 * The rule execution should produce the following output:
 * <ul>
 * <li>The credit limit of the customer is 3000
 * <li>The status of the invoice is paid.
 * </ul>
 * The second input to the rule execution set is:
 * <ul>
 * <li>An Invoice with an amount of 1500.
 * </ul>
 * The rule execution should produce the following output:
 * <ul>
 * <li>The credit limit of the customer is 1500
 * <li>The status of the invoices is paid.
 * </ul>
 * <p>
 * <b>Performs the following tests:</b><br>
 * <ul>
 * <li>Basic API tests. {@link #testStatefulRuleSession}
 * <ul>
 * <li>Create Stateful rule session
 * <li>Add and Remove and Get objects
 * <li>Execute rules
 * <li>Get processed objects
 * <li>Object filtering
 * <li>Reset session
 * </ul>
 * </ul>
 *
 * @version 1.0
 * @see javax.rules.StatefulRuleSession
 * @since JSR-94 1.0
 */
class StatefulRuleSessionTest {
    /**
     * Test the compliance for javax.rules.StatefulRuleSession.
     * Create a stateful rule session, add a Customer and Invoice
     * object and verify whether those objects can be retrieved be
     * their Handle. Remove the objects via the handle and reset the
     * session. Add the object (this time all at once via a List) and
     * execute the rules. Verify the results and add another Invoice
     * object. Verify the results again. This time also check whether
     * the state in the Customer object was correctly maintained. Reset
     * the session and add the objects one more time. Execute the rules
     * and this time apply an object filter to the result set.
     * <p>
     * <b>Description:</b><br>
     * <ul>
     * <li>Create Stateful rule session
     * <ul>
     * <li>Fail: If any error occurs while creating the stateful rule
     * session.
     * <li>Succeed: If a stateful rule session can successfully be
     * created.
     * </ul>
     * <li>Add and Remove and Get objects
     * <ul>
     * <li>Fail: If any of the add, get, and remove operations do not
     * return the expected results.
     * <li>Succeed: If add, get and remove are successfully handling the
     * rule session interaction.
     * </ul>
     * <li>Execute rules
     * <ul>
     * <li>Fail: If any error occurs while executing the rules.
     * <li>Succeed: If the execute rules is successful and the result
     * set is according to the specifications.
     * </ul>
     * <li>Get processed objects
     * <ul>
     * <li>Fail: If the result set is not in compliance with the
     * specifications.
     * <li>Succeed: If all the object can successfully be retrieved.
     * </ul>
     * <li>Object filtering
     * <ul>
     * <li>Fail: If not all objects in the result set are filtered
     * correctly.
     * <li>Succeed: If only the by the object filter specified objects
     * can be found in the result set.
     * </ul>
     * <li>Reset session
     * <ul>
     * <li>Fail: If any error occurs during the reset of the session.
     * <li>Succeed: If the reset is successful and no objects are found.
     * </ul>
     * </ul>
     *
     * @see TestCaseUtil#getStatefulRuleSession
     * @see TestObjectFilter
     * @see Customer
     * @see Invoice
     */
    @Test
    void testStatefulRuleSession() {
        try {
            StatefulRuleSession ruleSession =
                    TestCaseUtil.getStatefulRuleSession("stateful", "src/test/resources/TckRes1.java");
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

            // Add objects one by one.
            Handle customerHandle = ruleSession.addObject(inputCustomer);
            assert customerHandle != null;

            Handle invoiceHandle = ruleSession.addObject(inputInvoice);
            assert invoiceHandle != null;

            Object obj;

            // Retrieve the objects one by one.
            obj = ruleSession.getObject(customerHandle);
            assert obj != null;
            assert obj instanceof Customer;

            obj = ruleSession.getObject(invoiceHandle);
            assert obj != null;
            assert obj instanceof Invoice;

            // Retrieve all the objects.
            List<?> list = ruleSession.getObjects();
            assert list != null;
            assert list.size() == 2 : "Actual: " + list.size();

            Iterator<?> itr = list.iterator();

            Customer resultCustomer = null;
            Invoice resultInvoice = null;

            while (itr.hasNext()) {
                obj = itr.next();
                if (obj instanceof Customer)
                    resultCustomer = (Customer) obj;
                if (obj instanceof Invoice)
                    resultInvoice = (Invoice) obj;
            }

            // We should have a customer and an invoice.
            assert resultCustomer != null;
            assert resultInvoice != null;

            // Create a new Customer for update purposes.
            Customer updateCustomer = new Customer("update-test");
            updateCustomer.setCreditLimit(8000);

            // Update the customer object.
            ruleSession.updateObject(customerHandle, updateCustomer);

            // Retrieve the updated customer.
            obj = ruleSession.getObject(customerHandle);
            assert obj != null;
            assert obj instanceof Customer;
            updateCustomer = (Customer) obj;
            assert updateCustomer.getCreditLimit() == 8000 : "Actual:" + updateCustomer;

            // Get all handle test.
            List<?> handles = ruleSession.getHandles();
            assert handles != null;
            assert handles.size() == 2;

            itr = handles.iterator();

            resultCustomer = null;
            resultInvoice = null;

            while (itr.hasNext()) {
                obj = itr.next(); // Get the handle from the list
                obj = ruleSession.getObject((Handle) obj); // Get the object from the handle.
                if (obj instanceof Customer)
                    resultCustomer = (Customer) obj;
                if (obj instanceof Invoice)
                    resultInvoice = (Invoice) obj;
            }
            // We should have an invoice and the updated customer.
            assert resultCustomer != null;
            assert resultInvoice != null;

            // Remove the customer object.
            ruleSession.removeObject(customerHandle);

            // Retrieve all the objects and check whether not not the
            // customer has been removed.
            list = ruleSession.getObjects();
            assert list != null;
            assert list.size() == 1 : "Actual: " + list.size() + " vs expected 1";

            itr = list.iterator();

            resultCustomer = null;
            resultInvoice = null;

            while (itr.hasNext()) {
                obj = itr.next();
                if (obj instanceof Customer)
                    resultCustomer = (Customer) obj;
                if (obj instanceof Invoice)
                    resultInvoice = (Invoice) obj;
            }

            // We should only have an invoice.
            assert resultCustomer == null;
            assert resultInvoice != null;

            // Reset the session.
            ruleSession.reset();

            // Retrieve all the objects, nothing should be here. The
            // reset should have taken care of the removal.
            list = ruleSession.getObjects();
            assert list != null;
            assert list.size() == 0;

            // Add the objects all at once.
            handles = ruleSession.addObjects(input);
            assert handles != null;
            assert handles.size() == 2;


            // Execute the rules.
            ruleSession.executeRules();

            // Check the results (no object filtering).
            List<?> results = ruleSession.getObjects();
            assert results != null;
            assert results.size() == 2;

            itr = results.iterator();

            resultCustomer = null;
            resultInvoice = null;

            while (itr.hasNext()) {
                obj = itr.next();
                if (obj instanceof Customer)
                    resultCustomer = (Customer) obj;
                if (obj instanceof Invoice)
                    resultInvoice = (Invoice) obj;
            }

            // We should have a customer and an invoice.
            assert resultCustomer != null;
            assert resultInvoice != null;
            // Verify the results (although technically speaking we
            // shouldn't verify how the rule engine works.
            assert resultCustomer.getCreditLimit() == 3000;
            assert resultInvoice.getStatus().equals("paid");

            // Now add another object.
            Invoice secondInvoice = new Invoice("invoice2");
            secondInvoice.setAmount(1500);
            //Handle secondInvoiceHandle = ruleSession.addObject(secondInvoice);
            ruleSession.addObject(secondInvoice);

            // Execute the rules.
            ruleSession.executeRules();

            // Retrieve the results. (with an object filter).
            // Create the object filter.
            ObjectFilter customerFilter =
                    new TestObjectFilter(TestObjectFilter.CUSTOMER_FILTER);
            ObjectFilter invoiceFilter =
                    new TestObjectFilter(TestObjectFilter.INVOICE_FILTER);

            // Get the customer
            results = ruleSession.getObjects(customerFilter);
            assert results != null;
            assert results.size() == 1;

            itr = results.iterator();

            resultCustomer = null;
            resultInvoice = null;

            while (itr.hasNext()) {
                obj = itr.next();
                if (obj instanceof Customer)
                    resultCustomer = (Customer) obj;
                if (obj instanceof Invoice)
                    resultInvoice = (Invoice) obj;
            }

            // We should only have a customer.
            assert resultCustomer != null;
            assert resultInvoice == null;

            // Verify the results (although technically speaking we
            // shouldn't verify how the rule engine works.
            // The Customer's credit limit was down to 3000 in  the
            // first session, since we kept state it should now be down
            // to 1500.
            assert resultCustomer.getCreditLimit() == 1500;


            // Get the invoices (we should have 2 of them)
            results = ruleSession.getObjects(invoiceFilter);
            assert results != null;
            assert results.size() == 2;

            itr = results.iterator();

            resultCustomer = null;
            resultInvoice = null;

            while (itr.hasNext()) {
                obj = itr.next();
                if (obj instanceof Customer)
                    resultCustomer = (Customer) obj;
                if (obj instanceof Invoice)
                    resultInvoice = (Invoice) obj;
            }

            // We should only have invoices.
            assert resultCustomer == null;
            assert resultInvoice != null;

            // Reset the session.
            ruleSession.reset();

            // Release the session.
            ruleSession.release();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }
}
