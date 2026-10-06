package com.group49.support.util;
import com.group49.support.model.TicketEvent;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.*;
public final class TicketEventPublisher {
    private final CopyOnWriteArrayList<TicketObserver> observers = new CopyOnWriteArrayList<>();
    public void addObserver(TicketObserver observer) { observers.addIfAbsent(observer); }
    public void removeObserver(TicketObserver observer) { observers.remove(observer); }
    public boolean publish(TicketEvent event) {
        boolean success=true;
        for(var observer:observers) try { observer.update(event); }
        catch(Exception e) { success=false; Logger.getLogger(getClass().getName()).log(Level.WARNING,"Ticket saved, notification failed",e); }
        return success;
    }
}
