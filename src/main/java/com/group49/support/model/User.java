package com.group49.support.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public record User(int id, String fullName, String email, String username, String phone,
                   String role, String status, LocalDateTime createdAt) implements Serializable {
    public boolean isCustomer() { return "CUSTOMER".equals(role); }
    public boolean hasRole(String... roles) {
        for (String allowed : roles) if (role.equals(allowed)) return true;
        return false;
    }
    public String roleLabel() { return role.replace('_', ' '); }
    public int getId(){return id;} public String getFullName(){return fullName;} public String getEmail(){return email;}
    public String getUsername(){return username;} public String getPhone(){return phone;} public String getRole(){return role;}
    public String getStatus(){return status;} public LocalDateTime getCreatedAt(){return createdAt;} public String getRoleLabel(){return roleLabel();}
}
