package com.group49.support.model;

import java.time.LocalDateTime;

public record TicketMessage(int id, int ticketId, int senderId, String senderName, String senderRole,
                            String message, boolean internalNote, boolean read, LocalDateTime sentAt) {
    public int getId(){return id;} public int getTicketId(){return ticketId;} public int getSenderId(){return senderId;}
    public String getSenderName(){return senderName;} public String getSenderRole(){return senderRole;} public String getMessage(){return message;}
    public boolean isInternalNote(){return internalNote;} public boolean isRead(){return read;} public LocalDateTime getSentAt(){return sentAt;}
}
