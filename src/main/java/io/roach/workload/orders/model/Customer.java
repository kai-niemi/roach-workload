package io.roach.workload.orders.model;

import java.util.Date;
import java.util.UUID;

public class Customer {
    private UUID id;

    private String userName;

    private String firstName;

    private String lastName;

    private String telephone;

    private String email;

    private Address address = new Address();

    private Date createdTime = new Date();

    public Customer() {
    }

    public Customer(String userName) {
        this.userName = userName;
    }

    public UUID getId() {
        return id;
    }

    public Customer setId(UUID id) {
        this.id = id;
        return this;
    }

    public Customer setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public Customer setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public String getTelephone() {
        return telephone;
    }

    public Customer setTelephone(String telephone) {
        this.telephone = telephone;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public Customer setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public Customer setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public Customer setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public Address getAddress() {
        return address;
    }

    public Customer setAddress(Address address) {
        if (address == null) {
            address = new Address();
        }
        this.address = address;
        return this;
    }

    public Date getCreatedTime() {
        return new Date(createdTime.getTime());
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", telephone='" + telephone + '\'' +
                ", email='" + email + '\'' +
                ", address=" + address +
                ", createdTime=" + createdTime +
                '}';
    }
}
