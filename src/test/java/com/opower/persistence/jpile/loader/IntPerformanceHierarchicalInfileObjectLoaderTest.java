package com.opower.persistence.jpile.loader;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import com.google.common.io.Closeables;
import com.opower.persistence.jpile.AbstractIntTestForJPile;
import com.opower.persistence.jpile.sample.Customer;
import com.opower.persistence.jpile.sample.ObjectFactory;
import com.opower.persistence.jpile.sample.Product;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.IfProfileValue;

/**
 * @author amir.raminfar
 */
@IfProfileValue(name = "performance", value = "true")
public class IntPerformanceHierarchicalInfileObjectLoaderTest extends AbstractIntTestForJPile {
    private static final String CUSTOMER_SQL = "insert into customer (last_seen_on) values (?)";
    private static final String PRODUCT_SQL
            = "insert into product (customer_id, purchased_on, title, description, price) values (?, ?, ?, ?, ?)";
    private static final String CONTACT_SQL = "insert into contact (customer_id, first_name, last_name) values (?, ?, ?)";
    private static final String CONTACT_PHONE_SQL = "insert contact_phone (customer_id, phone) values (?, ?)";
    private static final int CUSTOMER_TO_GENERATE = 20000;

    Customer[] customers;

    @Before
    public void generateCustomers() throws Exception {
        customers = new Customer[CUSTOMER_TO_GENERATE];
        for(int i = 0; i < customers.length; i++) {
            customers[i] = ObjectFactory.newCustomer();
        }
    }

    @Test
    public void testWithPreparedStatement() throws SQLException {
        PreparedStatement customer = connection.prepareStatement(CUSTOMER_SQL);
        PreparedStatement product = connection.prepareStatement(PRODUCT_SQL);
        PreparedStatement contact = connection.prepareStatement(CONTACT_SQL);
        PreparedStatement phone = connection.prepareStatement(CONTACT_PHONE_SQL);

        long start = System.nanoTime();
        for(long i = 0, customersLength = customers.length; i < customersLength; i++) {
            Customer c = customers[((int) i)];
            c.setId(i + 1);

            customer.clearParameters();
            product.clearParameters();
            contact.clearParameters();
            phone.clearParameters();

            writeCustomer(customer, c);
            customer.executeUpdate();

            for(Product p : c.getProducts()) {
                writeProduct(product, c, p);
                product.executeUpdate();
            }

            writeContact(contact, c);
            contact.executeUpdate();

            writeContactPhone(phone, c);
            phone.executeUpdate();
        }
        customer.close();
        product.close();
        contact.close();
        phone.close();
        long elapsed = System.nanoTime() - start;

        System.out.printf("Total time to save %d customers was %d seconds with prepared statements.%n",
                          CUSTOMER_TO_GENERATE, 
                          TimeUnit.NANOSECONDS.toSeconds(elapsed));
        System.out.printf("Throughput for prepared statements was %d objects/second%n",
                          CUSTOMER_TO_GENERATE / TimeUnit.NANOSECONDS.toSeconds(elapsed));
    }
    
    @Test
    public void testWithJPile() {
        long start = System.nanoTime();

        Customer firstOne = ObjectFactory.newCustomer();
        hierarchicalInfileObjectLoader.persist(firstOne, customers);
        hierarchicalInfileObjectLoader.flush();
        
        long elapsed = System.nanoTime() - start;
        System.out.printf("Total time to save %d customers was %d seconds with jPile.%n",
                          CUSTOMER_TO_GENERATE,
                          TimeUnit.NANOSECONDS.toSeconds(elapsed));
        System.out.printf("Throughput for jPile was %d objects/second%n",
                          CUSTOMER_TO_GENERATE / TimeUnit.NANOSECONDS.toSeconds(elapsed));

    }

    private void writeContactPhone(PreparedStatement phone, Customer c) throws SQLException {
        phone.setLong(1, c.getId());
        phone.setString(2, c.getContact().getPhone());
    }

    private void writeContact(PreparedStatement contact, Customer c) throws SQLException {
        contact.setLong(1, c.getId());
        contact.setString(2, c.getContact().getFirstName());
        contact.setString(3, c.getContact().getLastName());
    }

    private void writeProduct(PreparedStatement product, Customer c, Product p) throws SQLException {
        product.setLong(1, c.getId());
        product.setDate(2, new Date(p.getPurchasedOn().getTime()));
        product.setString(3, p.getTitle());
        product.setString(4, p.getDescription());
        product.setBigDecimal(5, p.getPrice());
    }

    private void writeCustomer(PreparedStatement customer, Customer c) throws SQLException {
        customer.setDate(1, new Date(c.getLastSeenOn().getTime()));
    }
}
