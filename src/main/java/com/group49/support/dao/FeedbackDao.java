package com.group49.support.dao;

import com.group49.support.config.Database;
import com.group49.support.model.Feedback;
import com.group49.support.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FeedbackDao {
    public List<Feedback> list(User user) throws SQLException {
        String sql = "SELECT f.*,t.ticket_number,u.full_name customer_name FROM feedback f " +
                "JOIN tickets t ON t.ticket_id=f.ticket_id JOIN users u ON u.user_id=f.customer_id" +
                (user.isCustomer() ? " WHERE f.customer_id=? AND f.status<>'REMOVED'" : "") +
                " ORDER BY f.created_at DESC";
        List<Feedback> list = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (user.isCustomer()) statement.setInt(1, user.id());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) list.add(map(result));
            }
        }
        return list;
    }

    public void submit(int ticketId, int customerId, int rating, String comments) throws SQLException {
        String sql = "INSERT INTO feedback(ticket_id,customer_id,rating,comments) " +
                "SELECT ticket_id,customer_id,?,? FROM tickets WHERE ticket_id=? AND customer_id=? " +
                "AND status IN ('RESOLVED','CLOSED') AND NOT EXISTS(SELECT 1 FROM feedback WHERE ticket_id=?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, rating);
            statement.setString(2, comments);
            statement.setInt(3, ticketId);
            statement.setInt(4, customerId);
            statement.setInt(5, ticketId);
            if (statement.executeUpdate() != 1) throw new SQLException("Ticket is not eligible for feedback");
        }
    }

    public boolean updateByCustomer(int id, int customerId, int rating, String comments) throws SQLException {
        String sql = "UPDATE feedback SET rating=?,comments=?,updated_at=SYSDATETIME() " +
                "WHERE feedback_id=? AND customer_id=? AND status='NEW'";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, rating);
            statement.setString(2, comments);
            statement.setInt(3, id);
            statement.setInt(4, customerId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean deleteByCustomer(int id, int customerId) throws SQLException {
        String sql = "DELETE FROM feedback WHERE feedback_id=? AND customer_id=? AND status='NEW'";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, customerId);
            return statement.executeUpdate() == 1;
        }
    }

    public void respond(int id, String response, String status) throws SQLException {
        String sql = "UPDATE feedback SET response=?,status=?,updated_at=SYSDATETIME() WHERE feedback_id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, response);
            statement.setString(2, status);
            statement.setInt(3, id);
            statement.executeUpdate();
        }
    }

    public void removeByStaff(int id) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE feedback SET status='REMOVED',updated_at=SYSDATETIME() WHERE feedback_id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private Feedback map(ResultSet result) throws SQLException {
        return new Feedback(result.getInt("feedback_id"), result.getInt("ticket_id"),
                result.getString("ticket_number"), result.getInt("customer_id"), result.getString("customer_name"),
                result.getInt("rating"), result.getString("comments"), result.getString("response"),
                result.getString("status"), result.getTimestamp("created_at").toLocalDateTime());
    }
}
