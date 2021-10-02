package org.evrete.examples.run;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class XMLFacts {
    private static final DocumentBuilderFactory BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();
    private static final String CUSTOMER_TYPE_NAME = "Customer XML Type";

    public static void main(String[] args) throws Exception {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service.newKnowledge();

        // Declaring a new type associated with XML documents
        Type<Document> customerType = knowledge
                .getTypeResolver()
                .getOrDeclare(CUSTOMER_TYPE_NAME, Document.class);

        // Declaring the "active" field
        TypeField activeField = customerType
                .declareBooleanField(
                        "active",
                        doc -> Boolean.parseBoolean(doc.getDocumentElement().getAttribute("active"))
                );

        // Declaring the "name" field
        TypeField nameField = customerType
                .declareField(
                        "name",
                        String.class,
                        doc -> doc.getDocumentElement().getAttribute("name")
                );

        // Creating knowledge and session
        StatefulSession session = knowledge
                .newRule("Process active XML Customers")
                .forEach("$c", CUSTOMER_TYPE_NAME)
                .where("$c.active == true")
                .execute(ctx -> {
                    Document customer = ctx.get("$c");
                    System.out.printf(
                            "An active customer processed:%n\tsource='%s'\n",
                            asString(customer)
                    );
                })
                .newStatefulSession();

        Document customer1 = newCustomer("ABC Ltd", true);
        Document customer2 = newCustomer("XYZ Ltd", false);

        // Explicit type declaration during inserts
        session.insert(CUSTOMER_TYPE_NAME, customer1);
        session.insert(CUSTOMER_TYPE_NAME, customer2);

        session.fire();
        // Only one XML document will pass the 'active == true' filter
    }

    private static Document newCustomer(String name, boolean active) throws Exception {
        DocumentBuilder builder = BUILDER_FACTORY.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = doc.createElement("customer");
        root.setAttribute("name", name);
        root.setAttribute("active", String.valueOf(active));
        doc.appendChild(root);
        return doc;
    }

    private static String asString(Document doc) {
        try {
            Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (TransformerException e) {
            throw new IllegalStateException(e);
        }
    }
}
