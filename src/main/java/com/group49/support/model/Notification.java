package com.group49.support.model;

import java.time.LocalDateTime;

public record Notification(int id, int userId, String title, String message, String link,
                           boolean read, LocalDateTime createdAt) {
    public int getId(){return id;} public int getUserId(){return userId;} public String getTitle(){return title;}
    public String getMessage(){return message;} public String getLink(){return link;} public boolean isRead(){return read;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
