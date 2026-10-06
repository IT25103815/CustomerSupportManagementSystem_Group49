package com.group49.support.dao;

import com.group49.support.config.Database;
import com.group49.support.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class TicketDao {
    private static final String SELECT = "SELECT t.*,c.name category_name,u.full_name customer_name," +
            "a.full_name assignee_name FROM tickets t " +
            "JOIN ticket_categories c ON c.category_id=t.category_id " +
            "JOIN users u ON u.user_id=t.customer_id " +
            "LEFT JOIN users a ON a.user_id=t.assigned_to ";

    public List<Ticket> list(User user, String search, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT)
                .append(" WHERE (?='' OR t.ticket_number LIKE ? OR t.subject LIKE ?) AND (?='' OR t.status=?) ");
        if (user.isCustomer()) sql.append("AND t.customer_id=? ");
        else if (user.hasRole("SENIOR_CUSTOMER_SERVICE_OFFICER")) sql.append("AND (t.assigned_to=? OR t.assigned_to IS NULL) ");
        sql.append("ORDER BY CASE t.priority WHEN 'URGENT' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END,t.updated_at DESC");

        List<Ticket> items = new ArrayList<>();
        String like = "%" + search + "%";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setString(1, search);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setString(4, status);
            statement.setString(5, status);
            if (user.isCustomer() || user.hasRole("SENIOR_CUSTOMER_SERVICE_OFFICER")) statement.setInt(6, user.id());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) items.add(map(result));
            }
        }
        return items;
    }

    public Optional<Ticket> find(int id, User user) throws SQLException {
        String sql = SELECT + " WHERE t.ticket_id=?" + (user.isCustomer() ? " AND t.customer_id=?" : "");
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            if (user.isCustomer()) statement.setInt(2, user.id());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    public int create(int customerId, int categoryId, String subject, String description, String priority) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int id;
                String insert = "INSERT INTO tickets(ticket_number,customer_id,category_id,subject,description,priority) " +
                        "OUTPUT INSERTED.ticket_id VALUES('PENDING',?,?,?,?,?)";
                try (PreparedStatement statement = connection.prepareStatement(insert)) {
                    statement.setInt(1, customerId);
                    statement.setInt(2, categoryId);
                    statement.setString(3, subject);
                    statement.setString(4, description);
                    statement.setString(5, priority);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new SQLException("Ticket id was not returned");
                        id = result.getInt(1);
                    }
                }
                String number = "TKT-" + LocalDateTime.now().getYear() + "-" + String.format("%05d", id);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE tickets SET ticket_number=? WHERE ticket_id=?")) {
                    statement.setString(1, number);
                    statement.setInt(2, id);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO ticket_status_history(ticket_id,new_status,changed_by,note) VALUES(?,'OPEN',?,'Ticket created')")) {
                    statement.setInt(1, id);
                    statement.setInt(2, customerId);
                    statement.executeUpdate();
                }
                connection.commit();
                return id;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public boolean updateCustomerDetails(int id, int customerId, int categoryId, String subject,
                                         String description, String priority) throws SQLException {
        String sql = "UPDATE tickets SET category_id=?,subject=?,description=?,priority=?,updated_at=SYSDATETIME() " +
                "WHERE ticket_id=? AND customer_id=? AND status='OPEN' AND assigned_to IS NULL";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, categoryId);
            statement.setString(2, subject);
            statement.setString(3, description);
            statement.setString(4, priority);
            statement.setInt(5, id);
            statement.setInt(6, customerId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean cancelByCustomer(int id, int customerId) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String sql = "UPDATE tickets SET status='CANCELLED',updated_at=SYSDATETIME() " +
                        "WHERE ticket_id=? AND customer_id=? AND status='OPEN' AND assigned_to IS NULL";
                int changed;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, id);
                    statement.setInt(2, customerId);
                    changed = statement.executeUpdate();
                }
                if (changed == 1) {
                    try (PreparedStatement history = connection.prepareStatement(
                            "INSERT INTO ticket_status_history(ticket_id,old_status,new_status,changed_by,note) " +
                                    "VALUES(?,'OPEN','CANCELLED',?,'Cancelled by customer')")) {
                        history.setInt(1, id);
                        history.setInt(2, customerId);
                        history.executeUpdate();
                    }
                }
                connection.commit();
                return changed == 1;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public boolean deleteCancelled(int id) throws SQLException {
        String sql = "DELETE FROM tickets WHERE ticket_id=? AND status='CANCELLED' " +
                "AND NOT EXISTS(SELECT 1 FROM feedback WHERE feedback.ticket_id=tickets.ticket_id)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    public void update(int id, String status, String priority, Integer assignedTo, String note, int changedBy) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String oldStatus;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT status FROM tickets WHERE ticket_id=?")) {
                    statement.setInt(1, id);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new SQLException("Ticket not found");
                        oldStatus = result.getString(1);
                    }
                }
                String sql = "UPDATE tickets SET status=?,priority=?,assigned_to=?,updated_at=SYSDATETIME()," +
                        "resolved_at=CASE WHEN ?='RESOLVED' THEN COALESCE(resolved_at,SYSDATETIME()) ELSE resolved_at END," +
                        "closed_at=CASE WHEN ?='CLOSED' THEN COALESCE(closed_at,SYSDATETIME()) ELSE closed_at END WHERE ticket_id=?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, status);
                    statement.setString(2, priority);
                    if (assignedTo == null) statement.setNull(3, Types.INTEGER); else statement.setInt(3, assignedTo);
                    statement.setString(4, status);
                    statement.setString(5, status);
                    statement.setInt(6, id);
                    statement.executeUpdate();
                }
                if (!Objects.equals(oldStatus, status) || (note != null && !note.isBlank())) {
                    try (PreparedStatement history = connection.prepareStatement(
                            "INSERT INTO ticket_status_history(ticket_id,old_status,new_status,changed_by,note) VALUES(?,?,?,?,?)")) {
                        history.setInt(1, id);
                        history.setString(2, oldStatus);
                        history.setString(3, status);
                        history.setInt(4, changedBy);
                        history.setString(5, note);
                        history.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public List<Map<String, Object>> history(int ticketId) throws SQLException {
        List<Map<String, Object>> items = new ArrayList<>();
        String sql = "SELECT h.*,u.full_name FROM ticket_status_history h JOIN users u ON u.user_id=h.changed_by " +
                "WHERE ticket_id=? ORDER BY changed_at DESC";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ticketId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("oldStatus", result.getString("old_status"));
                    item.put("newStatus", result.getString("new_status"));
                    item.put("name", result.getString("full_name"));
                    item.put("note", result.getString("note"));
                    item.put("date", result.getTimestamp("changed_at").toLocalDateTime());
                    items.add(item);
                }
            }
        }
        return items;
    }

    public List<Ticket> eligibleForFeedback(int customerId) throws SQLException {
        List<Ticket> items = new ArrayList<>();
        String sql = SELECT + " WHERE t.customer_id=? AND t.status IN ('RESOLVED','CLOSED') " +
                "AND NOT EXISTS(SELECT 1 FROM feedback f WHERE f.ticket_id=t.ticket_id) ORDER BY t.updated_at DESC";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) items.add(map(result));
            }
        }
        return items;
    }

    public DashboardStats stats(User user, long unread) throws SQLException {
        String where = user.isCustomer() ? " WHERE customer_id=?" : "";
        Map<String, Long> statuses = new LinkedHashMap<>();
        Map<String, Long> priorities = new LinkedHashMap<>();
        try (Connection connection = Database.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT status,COUNT(*) total FROM tickets" + where + " GROUP BY status")) {
                if (user.isCustomer()) statement.setInt(1, user.id());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) statuses.put(result.getString(1), result.getLong(2));
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT priority,COUNT(*) total FROM tickets" + where + " GROUP BY priority")) {
                if (user.isCustomer()) statement.setInt(1, user.id());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) priorities.put(result.getString(1), result.getLong(2));
                }
            }
            double satisfaction = 0;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COALESCE(AVG(CAST(rating AS FLOAT)),0) FROM feedback WHERE status<>'REMOVED'" +
                            (user.isCustomer() ? " AND customer_id=?" : ""))) {
                if (user.isCustomer()) statement.setInt(1, user.id());
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) satisfaction = result.getDouble(1);
                }
            }
            long total = statuses.values().stream().mapToLong(Long::longValue).sum();
            return new DashboardStats(total, statuses.getOrDefault("OPEN", 0L),
                    statuses.getOrDefault("IN_PROGRESS", 0L), statuses.getOrDefault("ESCALATED", 0L),
                    statuses.getOrDefault("RESOLVED", 0L), statuses.getOrDefault("CLOSED", 0L), unread,
                    satisfaction, statuses, priorities);
        }
    }

    public List<Map<String, Object>> categories() throws SQLException {
        return simpleOptions("SELECT category_id id,name FROM ticket_categories ORDER BY name");
    }

    private List<Map<String, Object>> simpleOptions(String sql) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", result.getInt("id"));
                item.put("name", result.getString("name"));
                list.add(item);
            }
        }
        return list;
    }

    private Ticket map(ResultSet result) throws SQLException {
        Integer assigned = (Integer) result.getObject("assigned_to");
        return new Ticket(result.getInt("ticket_id"), result.getString("ticket_number"),
                result.getInt("customer_id"), result.getString("customer_name"), result.getInt("category_id"),
                result.getString("category_name"), assigned, result.getString("assignee_name"),
                result.getString("subject"), result.getString("description"), result.getString("priority"),
                result.getString("status"), result.getTimestamp("created_at").toLocalDateTime(),
                result.getTimestamp("updated_at").toLocalDateTime());
    }
}
