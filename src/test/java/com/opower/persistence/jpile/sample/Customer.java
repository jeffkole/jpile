package com.opower.persistence.jpile.sample;

import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * @author amir.raminfar
 */
@Entity
@Table
public class Customer {
    private Long id;
    private Contact contact;
    private List<Product> products;
    private Date lastSeenOn;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @OneToMany(mappedBy = "customer")
    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        for(Product product : products) {
            product.setCustomer(this);
        }
        this.products = products;
    }

    @OneToOne
    @PrimaryKeyJoinColumn
    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        contact.setCustomer(this);
        this.contact = contact;
    }

    @Column(name = "last_seen_on")
    public Date getLastSeenOn() {
        return lastSeenOn;
    }

    public void setLastSeenOn(Date lastSeenOn) {
        this.lastSeenOn = lastSeenOn;
    }
}
