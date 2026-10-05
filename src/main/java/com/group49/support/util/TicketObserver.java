package com.group49.support.util;
import com.group49.support.model.TicketEvent;
public interface TicketObserver { void update(TicketEvent event) throws Exception; }
