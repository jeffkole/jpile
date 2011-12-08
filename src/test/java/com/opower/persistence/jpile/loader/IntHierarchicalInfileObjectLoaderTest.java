package com.opower.persistence.jpile.loader;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import com.opower.persistence.jpile.AbstractIntTestForJPile;
import com.opower.persistence.jpile.sample.Customer;
import com.opower.persistence.jpile.sample.ObjectFactory;
import com.opower.persistence.jpile.sample.Product;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * @author amir.raminfar
 */
public class IntHierarchicalInfileObjectLoaderTest extends AbstractIntTestForJPile {
    @Test
    public void testSingleCustomer() throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Customer expected = ObjectFactory.newCustomer();
        hierarchicalInfileObjectLoader.persist(expected);
        hierarchicalInfileObjectLoader.flush();
        Map<String, Object> customer = simpleJdbcTemplate.queryForMap("select * from customer");
        Map<String, Object> contact = simpleJdbcTemplate.queryForMap("select * from contact");
        Map<String, Object> phone = simpleJdbcTemplate.queryForMap("select * from contact_phone");
        List<Map<String, Object>> products = simpleJdbcTemplate.queryForList("select * from product");

        assertEquals(simpleDateFormat.format(expected.getLastSeenOn()), simpleDateFormat.format(customer.get("last_seen_on")));
        assertEquals(expected.getId(), customer.get("id"));
        assertEquals(expected.getId(), contact.get("customer_id"));
        assertEquals(expected.getContact().getFirstName(), contact.get("first_name"));
        assertEquals(expected.getContact().getLastName(), contact.get("last_name"));
        assertEquals(expected.getId(), phone.get("customer_id"));
        assertEquals(expected.getContact().getPhone(), phone.get("phone"));
        assertEquals(expected.getProducts().size(), products.size());


        for(int i = 0, productsSize = expected.getProducts().size(); i < productsSize; i++) {
            Product expectedProduct = expected.getProducts().get(i);
            Map<String, Object> actualMap = products.get(i);
            assertEquals(expectedProduct.getId(), actualMap.get("id"));
            assertEquals(expected.getId().intValue(), actualMap.get("customer_id"));
            assertEquals(expectedProduct.getTitle(), actualMap.get("title"));
            assertEquals(expectedProduct.getDescription(), actualMap.get("description"));
            assertEquals(expectedProduct.getPrice().doubleValue(), actualMap.get("price"));
            assertEquals(simpleDateFormat.format(expectedProduct.getPurchasedOn()),
                         simpleDateFormat.format(actualMap.get("purchased_on")));
        }

    }

    @Test
    public void testHundredCustomers() {
        for(int i = 0; i < 100; i++) {
            hierarchicalInfileObjectLoader.persist(ObjectFactory.newCustomer());
        }
    }
}
