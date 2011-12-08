package com.opower.persistence.jpile.sample;

import java.math.BigDecimal;
import java.util.Date;
import com.google.common.collect.ImmutableList;

/**
 * @author amir.raminfar
 */
public class ObjectFactory {
    public static Customer newCustomer() {
        Customer customer = new Customer();
        customer.setContact(newContact());
        customer.setLastSeenOn(new Date());
        customer.setProducts(ImmutableList.of(newProduct(), newProduct(), newProduct(), newProduct()));

        return customer;
    }

    public static Contact newContact() {
        Contact contact = new Contact();
        contact.setFirstName("John");
        contact.setLastName("Smith");
        contact.setPhone("1234445566");

        return contact;
    }

    public static Product newProduct() {
        Product product = new Product();
        product.setDescription("This is a short description about this product");
        product.setPrice(BigDecimal.valueOf(1.23));
        product.setPurchasedOn(new Date());
        product.setTitle("Title of an awesome product");

        return product;
    }
}
