package com.printhub.model;

import java.util.LinkedHashSet;
import java.util.Set;

/** Someone the shop buys supplies from, and which supplies they provide. */
public class Supplier {

    private final String id;
    private String name;
    private String address;
    private String phone;
    private final Set<String> supplyIds = new LinkedHashSet<>();

    public Supplier(String id, String name, String address, String phone) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /** Ids of the supplies this supplier provides. */
    public Set<String> getSupplyIds() {
        return supplyIds;
    }

    @Override
    public String toString() {
        return name;
    }
}
