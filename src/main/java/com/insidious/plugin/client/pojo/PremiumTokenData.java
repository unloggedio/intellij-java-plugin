package com.insidious.plugin.client.pojo;

import java.util.Date;

public class PremiumTokenData {

    private String token;
    private String id;
    private String customerEmail;
    private long expiresAt;

    // Constructors
    public PremiumTokenData() {
    }

    public PremiumTokenData(String token, String id, String customerEmail, long expiresAt) {
        this.token = token;
        this.id = id;
        this.customerEmail = customerEmail;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public String toString() {
        return "PremiumTokenData{" +
                "token='" + token + '\'' +
                ", id='" + id + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", expiresAt=" + new Date(expiresAt) +
                '}';
    }
}
