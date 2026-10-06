package com.group49.support.model;

import java.util.Map;

public record DashboardStats(long total, long open, long inProgress, long escalated, long resolved,
                             long closed, long unread, double satisfaction, Map<String, Long> byStatus,
                             Map<String, Long> byPriority) {
    public long getTotal(){return total;} public long getOpen(){return open;} public long getInProgress(){return inProgress;}
    public long getEscalated(){return escalated;} public long getResolved(){return resolved;} public long getClosed(){return closed;}
    public long getUnread(){return unread;} public double getSatisfaction(){return satisfaction;} public Map<String,Long> getByStatus(){return byStatus;}
    public Map<String,Long> getByPriority(){return byPriority;}
}
