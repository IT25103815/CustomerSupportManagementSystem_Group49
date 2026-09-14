package com.group49.support.model;

import java.time.LocalDateTime;

public record Feedback(int id, int ticketId, String ticketNumber, int customerId, String customerName,
                       int rating, String comments, String response, String status, LocalDateTime createdAt) {
    public int getId(){return id;} public int getTicketId(){return ticketId;} public String getTicketNumber(){return ticketNumber;}
    public int getCustomerId(){return customerId;} public String getCustomerName(){return customerName;} public int getRating(){return rating;}
    public String getComments(){return comments;} public String getResponse(){return response;} public String getStatus(){return status;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
