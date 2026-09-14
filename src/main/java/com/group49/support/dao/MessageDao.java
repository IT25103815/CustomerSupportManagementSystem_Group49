package com.group49.support.dao;

import com.group49.support.config.Database;
import com.group49.support.model.TicketMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** DAO for the Communication part of Communication & Notification Management. */
public class MessageDao {
    public void create(int ticketId, int senderId, String message, boolean internalNote) throws SQLException {
        String sql = "INSERT INTO ticket_messages(ticket_id,sender_id,message,internal_note) VALUES(?,?,?,?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ticketId);
            statement.setInt(2, senderId);
            statement.setString(3, message);
            statement.setBoolean(4, internalNote);
            statement.executeUpdate();
        }
    }

    public List<TicketMessage> listByTicket(int ticketId, boolean includeInternal) throws SQLException {
        String sql = "SELECT m.*,u.full_name sender_name,u.role sender_role FROM ticket_messages m " +
                "JOIN users u ON u.user_id=m.sender_id WHERE m.ticket_id=?" +
                (includeInternal ? "" : " AND m.internal_note=0") + " ORDER BY m.sent_at";
        List<TicketMessage> items = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ticketId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    items.add(new TicketMessage(result.getInt("message_id"), result.getInt("ticket_id"),
                            result.getInt("sender_id"), result.getString("sender_name"), result.getString("sender_role"),
                            result.getString("message"), result.getBoolean("internal_note"), result.getBoolean("is_read"),
                            result.getTimestamp("sent_at").toLocalDateTime()));
                }
            }
        }
        return items;
    }

    public void delete(int messageId, int senderId) throws SQLException {
        String sql = "DELETE FROM ticket_messages WHERE message_id=? AND sender_id=? AND internal_note=1";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, messageId);
            statement.setInt(2, senderId);
            statement.executeUpdate();
        }
    }
}
