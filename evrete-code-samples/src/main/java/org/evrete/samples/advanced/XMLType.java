package org.evrete.samples.advanced;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class XMLType {
    private static final DocumentBuilderFactory BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();
    private static final String CUSTOMER_TYPE_NAME = "Customer XML Type";

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service.newKnowledge();

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

        StatefulSession session = knowledge
                .newRule("Process active XML Customers")
                .forEach("$c", CUSTOMER_TYPE_NAME)
                .where("$c.active == true")
                .execute(ctx -> {
                            Document customer = ctx.get("$c");
                            boolean activeValue = activeField.readValue(customer);
                            String nameValue = nameField.readValue(customer);
                            System.out.printf("An active customer processed:\n\tname='%s', \n\tactive='%s', \n\tsource='%s'\n", nameValue, activeValue, asString(customer));
                        }
                )
                .createSession();

        Document customer1 = newCustomer("ABC Ltd", true);
        Document customer2 = newCustomer("XYZ Ltd", false);

        session.insert(CUSTOMER_TYPE_NAME, customer1);
        session.insert(CUSTOMER_TYPE_NAME, customer2);

        System.out.println("Firing rules...");
        session.fire();

    }

    private static Document newCustomer(String name, boolean active) {
        try {
            DocumentBuilder builder = BUILDER_FACTORY.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element root = doc.createElement("customer");
            root.setAttribute("name", name);
            root.setAttribute("active", String.valueOf(active));
            doc.appendChild(root);
            System.out.println("Customer XML created: " + asString(doc));
            return doc;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
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
