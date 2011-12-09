package com.opower.persistence.jpile.loader;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.opower.persistence.jpile.AbstractIntTestForJPile;
import com.opower.persistence.jpile.sample.Contact;
import com.opower.persistence.jpile.sample.Customer;
import com.opower.persistence.jpile.sample.ObjectFactory;
import com.opower.persistence.jpile.sample.Product;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.IfProfileValue;

import static junit.framework.Assert.assertEquals;

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
    private static final int CUSTOMER_TO_GENERATE = 25000;

    Customer[] customers;

    @Before
    public void generateCustomers() throws Exception {
        customers = new Customer[CUSTOMER_TO_GENERATE];
        for(int i = 0; i < customers.length; i++) {
            customers[i] = ObjectFactory.newCustomer();
        }
    }

    @After
    public void assertNumberOfCustomer() {
        assertEquals(CUSTOMER_TO_GENERATE, simpleJdbcTemplate.queryForInt("select count(*) from customer"));
    }

    @Test
    public void testWithPreparedStatement() throws SQLException {
        final PreparedStatement customer = connection.prepareStatement(CUSTOMER_SQL);
        final PreparedStatement product = connection.prepareStatement(PRODUCT_SQL);
        final PreparedStatement contact = connection.prepareStatement(CONTACT_SQL);
        final PreparedStatement phone = connection.prepareStatement(CONTACT_PHONE_SQL);

        doWithInTimedBlock(new Runnable() {
            @Override
            public void run() {
                try {
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
                }
                catch(SQLException e) {
                    throw Throwables.propagate(e);
                }
            }
        }, "Prepared Statements");

        customer.close();
        product.close();
        contact.close();
        phone.close();

    }


    @Test
    public void testWithHibernate() {
        final SessionFactory sessionFactory = new Configuration().configure()
                                                                 .addAnnotatedClass(Customer.class)
                                                                 .addAnnotatedClass(Contact.class)
                                                                 .addAnnotatedClass(Product.class)
                                                                 .buildSessionFactory();


        doWithInTimedBlock(new Runnable() {
            public void run() {
                Session session = sessionFactory.openSession();
                session.beginTransaction();
                for(Customer customer : customers) {
                    session.save(customer);
                }
                session.flush();
                session.getTransaction().commit();
                session.close();
            }
        }, "Hibernate");
    }

    @Test
    public void testWithJPile() {
        doWithInTimedBlock(new Runnable() {
            @Override
            public void run() {
                hierarchicalInfileObjectLoader.persist(customers[0], (Object[])customers);
                hierarchicalInfileObjectLoader.flush();
            }
        }, "jPile");
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

    private void doWithInTimedBlock(Runnable runnable, String name) {
        long start = System.nanoTime();
        runnable.run();
        long elapsed = System.nanoTime() - start;
        System.out.println(Strings.repeat("=", 100));
        System.out.printf("Total time to save %d customers was %d seconds with %s.%n",
                          CUSTOMER_TO_GENERATE,
                          TimeUnit.NANOSECONDS.toSeconds(elapsed),
                          name);
        System.out.printf("Throughput for %s was %d objects/second%n",
                          name,
                          CUSTOMER_TO_GENERATE / TimeUnit.NANOSECONDS.toSeconds(elapsed));
        System.out.println(Strings.repeat("=", 100));
        System.out.println();
    }
}
