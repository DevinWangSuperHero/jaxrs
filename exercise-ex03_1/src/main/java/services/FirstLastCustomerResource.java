package services;

import domain.Customer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.annotation.PostConstruct;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FirstLastCustomerResource  {
    private Map<String, Customer> customerDB =
            new ConcurrentHashMap<String, Customer>();

    @GET
    @Path("/{first}-{last}")
    @Produces("application/xml")
    public StreamingOutput getCustomer(@PathParam("first") String first, @PathParam("last") String last) {
        final Customer customer = customerDB.get(first + "-" + last);
        if (customer == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return new StreamingOutput() {
            @Override
            public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                outputCustomer(outputStream,customer);
            }
        };
    }

    @PUT
    @Path("{first}-{last}")
    @Produces("application/xml")
    public void updateCustomer(@PathParam("first") String first, @PathParam("last") String last, InputStream is) {
        Customer update = readCustomer(is);
        Customer current = customerDB.get(first+"-"+last);
        if (current == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
//            customerDB.put(first+"-"+last,update);
        }
        current.setFirstName(update.getFirstName());
        current.setLastName(update.getLastName());
        current.setStreet(update.getStreet());
        current.setState(update.getState());
        current.setZip(update.getZip());
        current.setCountry(update.getCountry());
    }

    @POST
    @Consumes("application/xml")
    public Response createCustomer(InputStream is) {
        Customer customer = readCustomer(is);
        String index = customer.getFirstName() + "-" + customer.getLastName();
        customerDB.put(index,customer);
        return Response.created(URI.create("/customer/northamerica-db/"+index)).build();
    }

    public void outputCustomer(OutputStream os, Customer cust) {
        PrintStream writer = new PrintStream(os);
        writer.println("<customer id=\"" + cust.getId() + "\">");
        writer.println("   <first-name>" + cust.getFirstName() + "</first-name>");
        writer.println("   <last-name>" + cust.getLastName() + "</last-name>");
        writer.println("   <street>" + cust.getStreet() + "</street>");
        writer.println("   <city>" + cust.getCity() + "</city>");
        writer.println("   <state>" + cust.getState() + "</state>");
        writer.println("   <zip>" + cust.getZip() + "</zip>");
        writer.println("   <country>" + cust.getCountry() + "</country>");
        writer.println("</customer>");
    }

    protected Customer readCustomer(InputStream is) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            Element root = doc.getDocumentElement();
            Customer customer = new Customer();
            if (root.getAttribute("id") != null && !root.getAttribute("id").trim().equals("")) {
                customer.setId(Integer.valueOf(root.getAttribute("id")));
            }
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                if (element.getTagName().equals("first-name")) {
                    customer.setFirstName(element.getTextContent());
                } else if (element.getTagName().equals("last-name")) {
                    customer.setLastName(element.getTextContent());
                } else if (element.getTagName().equals("street")) {
                    customer.setStreet(element.getTextContent());
                } else if (element.getTagName().equals("city")) {
                    customer.setCity(element.getTextContent());
                } else if (element.getTagName().equals("state")) {
                    customer.setState(element.getTextContent());
                } else if (element.getTagName().equals("zip")) {
                    customer.setZip(element.getTextContent());
                } else if (element.getTagName().equals("country")) {
                    customer.setCountry(element.getTextContent());
                }
            }
            return customer;
        } catch (Exception e) {
            throw new WebApplicationException(e,Response.Status.BAD_REQUEST);
        }
    }
}
