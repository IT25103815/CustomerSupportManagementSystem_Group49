package com.group49.support.model;

import java.time.LocalDateTime;

public record Faq(int id, int categoryId, String categoryName, String question, String answer,
                  boolean published, String authorName, LocalDateTime updatedAt) {
    public int getId(){return id;} public int getCategoryId(){return categoryId;} public String getCategoryName(){return categoryName;}
    public String getQuestion(){return question;} public String getAnswer(){return answer;} public boolean isPublished(){return published;}
    public String getAuthorName(){return authorName;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
