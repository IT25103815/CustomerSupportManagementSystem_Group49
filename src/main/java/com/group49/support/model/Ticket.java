package com.group49.support.model;

import java.time.LocalDateTime;

public record Ticket(int id, String number, int customerId, String customerName, int categoryId,
                     String categoryName, Integer assignedTo, String assigneeName, String subject,
                     String description, String priority, String status, LocalDateTime createdAt,
                     LocalDateTime updatedAt) {
    public int getId(){return id;} public String getNumber(){return number;} public int getCustomerId(){return customerId;}
    public String getCustomerName(){return customerName;} public int getCategoryId(){return categoryId;} public String getCategoryName(){return categoryName;}
    public Integer getAssignedTo(){return assignedTo;} public String getAssigneeName(){return assigneeName;} public String getSubject(){return subject;}
    public String getDescription(){return description;} public String getPriority(){return priority;} public String getStatus(){return status;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
