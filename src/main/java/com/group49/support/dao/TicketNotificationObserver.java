package com.group49.support.dao;
import com.group49.support.util.TicketObserver;
import com.group49.support.model.TicketEvent;
public final class TicketNotificationObserver implements TicketObserver {
    private final NotificationDao notifications = new NotificationDao();
    public void update(TicketEvent event) throws Exception {
        if(event.recipient()>0) notifications.create(event.recipient(),event.title(),event.message(),"/tickets?action=view&id="+event.ticketId());
    }
}
