package services;

import domain.Customer;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import sun.util.resources.cldr.so.CurrencyNames_so;

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
import java.util.concurrent.atomic.AtomicInteger;

@Path("/customers")
@Slf4j
public class CustomerResource {
    private Map<Integer, Customer> customerDB = new ConcurrentHashMap<Integer, Customer>();
    private AtomicInteger idCounter = new AtomicInteger();

    public CustomerResource() {
    }

    @POST
    @Consumes("application/xml")
    public Response createCustomer(InputStream is) {
        Customer customer = readCustomer(is);
        customer.setId(idCounter.incrementAndGet());
        customerDB.put(customer.getId(),customer);
        log.info("Created customer " + customer.getId());
        return Response.created(URI.create("/customers/europe-db/"+ customer.getId())).build();
    }

    @GET
    @Path("/{id: \\d+}")
    @Produces("application/xml")
    public Customer getCustomer(@BeanParam CustomerInput customerInput) {
        final Customer customer = customerDB.get(customerInput.getId());
        if (customer == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return customer;
    }

    @GET
    @Path("{first:[a-zA-Z]+}-{last: [a-zA-Z]+}")
    public StreamingOutput getCustomerFirstLast(@PathParam("first") String first, @PathParam("last") String last) {
        Customer found = null;
        for (Customer customer:customerDB.values()) {
            if (customer.getFirstName().equals(first) && customer.getLastName().equals(last)) {
                found = customer;
                break;
            }
        }
        if (found == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        final Customer customer = found;
        return new StreamingOutput() {
            @Override
            public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                outputCustomer(outputStream,customer);
            }
        };
    }

    @PUT
    @Path("/{id : \\d+}")
    @Consumes("application/xml")
    public void updateCustomer(@PathParam("id") int id, InputStream is) {
        Customer customer = readCustomer(is);
        Customer current = customerDB.get(id);
        if (current == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        current.setFirstName(customer.getFirstName());
        current.setLastName(current.getLastName());
        current.setStreet(current.getStreet());
        current.setState(customer.getState());
        current.setZip(customer.getZip());
        current.setCountry(customer.getCountry());
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
