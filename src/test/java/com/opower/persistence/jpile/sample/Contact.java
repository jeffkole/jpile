package com.opower.persistence.jpile.sample;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;

/**
 * @author amir.raminfar
 */
@Entity
@Table
@SecondaryTables(@SecondaryTable(name = "contact_phone", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "customer_id")}))
public class Contact {
    private Long id;
    private Customer customer;
    private String firstName;
    private String lastName;
    private String phone;


    @OneToOne
    @PrimaryKeyJoinColumn
    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    @Id
    @Column(name = "customer_id")
    public Long getId() {
        return id;
    }

    @Column(name = "phone", table = "contact_phone")
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Column(name = "last_name")
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Column(name = "first_name")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
}
