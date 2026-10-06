package com.group49.support.model;

import java.time.LocalDateTime;

/** Metadata for a screenshot attached to a support ticket.
 *  The image itself is stored in the server upload directory, not inside SQL Server.
 */
public record TicketAttachment(int id, int ticketId, String originalName, String storedName,
                               String contentType, long fileSize, int uploadedBy,
                               String uploaderName, LocalDateTime uploadedAt) {
    public int getId(){ return id; }
    public int getTicketId(){ return ticketId; }
    public String getOriginalName(){ return originalName; }
    public String getStoredName(){ return storedName; }
    public String getContentType(){ return contentType; }
    public long getFileSize(){ return fileSize; }
    public int getUploadedBy(){ return uploadedBy; }
    public String getUploaderName(){ return uploaderName; }
    public LocalDateTime getUploadedAt(){ return uploadedAt; }
    public long getFileSizeKb(){ return Math.max(1, (fileSize + 1023) / 1024); }
}
