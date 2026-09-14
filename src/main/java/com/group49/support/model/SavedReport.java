package com.group49.support.model;

import java.time.LocalDateTime;

public record SavedReport(int id, String name, String reportType, String filters, int createdBy,
                          String creatorName, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public int getId(){return id;}
    public String getName(){return name;}
    public String getReportType(){return reportType;}
    public String getFilters(){return filters;}
    public int getCreatedBy(){return createdBy;}
    public String getCreatorName(){return creatorName;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
}
