package com.group49.support.dao;

import com.group49.support.config.Database;
import com.group49.support.model.User;
import com.group49.support.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    public Optional<User> authenticate(String login, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE (username=? OR email=?) AND status='ACTIVE'";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            statement.setString(2, login);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next() && PasswordUtil.verify(password, result.getString("password_hash"))) {
                    return Optional.of(map(result));
                }
            }
        }
        return Optional.empty();
    }

    public User createCustomer(String name, String email, String username, String phone, String password) throws SQLException {
        return create(name, email, username, phone, password, "CUSTOMER", "ACTIVE");
    }

    public User create(String name, String email, String username, String phone, String password,
                       String role, String status) throws SQLException {
        String sql = "INSERT INTO users(full_name,email,username,password_hash,phone,role,status) " +
                "OUTPUT INSERTED.user_id VALUES(?,?,?,?,?,?,?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, username);
            statement.setString(4, PasswordUtil.hash(password));
            statement.setString(5, phone);
            statement.setString(6, role);
            statement.setString(7, status);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return findById(result.getInt(1)).orElseThrow();
            }
        }
        throw new SQLException("User account was not created");
    }

    public boolean exists(String email, String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email=? OR username=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setString(2, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) > 0;
            }
        }
    }

    public boolean emailUsedByAnother(String email, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email=? AND user_id<>?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setInt(2, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) > 0;
            }
        }
    }

    public Optional<User> findById(int id) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE user_id=?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    public List<User> list(String search) throws SQLException {
        String sql = "SELECT * FROM users WHERE (?='' OR full_name LIKE ? OR email LIKE ? OR username LIKE ?) ORDER BY created_at DESC";
        List<User> users = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String like = "%" + search + "%";
            statement.setString(1, search);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setString(4, like);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) users.add(map(result));
            }
        }
        return users;
    }

    public List<User> supportStaff() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role IN " +
                "('SENIOR_CUSTOMER_SERVICE_OFFICER','OPERATIONS_EXECUTIVE','CUSTOMER_SUPPORT_MANAGER') " +
                "AND status='ACTIVE' ORDER BY full_name";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) users.add(map(result));
        }
        return users;
    }

    public void updateStatusAndRole(int id, String role, String status) throws SQLException {
        String sql = "UPDATE users SET role=?,status=?,updated_at=SYSDATETIME() WHERE user_id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role);
            statement.setString(2, status);
            statement.setInt(3, id);
            statement.executeUpdate();
        }
    }

    public void updateProfile(int id, String name, String email, String phone) throws SQLException {
        String sql = "UPDATE users SET full_name=?,email=?,phone=?,updated_at=SYSDATETIME() WHERE user_id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, phone);
            statement.setInt(4, id);
            statement.executeUpdate();
        }
    }

    public boolean verifyPassword(int userId, String password) throws SQLException {
        String sql = "SELECT password_hash FROM users WHERE user_id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && PasswordUtil.verify(password, result.getString(1));
            }
        }
    }

    public void updatePassword(int userId, String password) throws SQLException {
        String sql = "UPDATE users SET password_hash=?,updated_at=SYSDATETIME() WHERE user_id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, PasswordUtil.hash(password));
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    /**
     * Deletes a user only when the account has no business records linked to it.
     * This makes a real DELETE operation safe for newly-created/test accounts.
     */
    public boolean deleteIfUnused(int id) throws SQLException {
        String references = "SELECT " +
                "(SELECT COUNT(*) FROM tickets WHERE customer_id=? OR assigned_to=?) + " +
                "(SELECT COUNT(*) FROM feedback WHERE customer_id=?) + " +
                "(SELECT COUNT(*) FROM faqs WHERE created_by=?) + " +
                "(SELECT COUNT(*) FROM ticket_messages WHERE sender_id=?) + " +
                "(SELECT COUNT(*) FROM ticket_status_history WHERE changed_by=?) + " +
                "(SELECT COUNT(*) FROM saved_reports WHERE created_by=?) + " +
                "(SELECT COUNT(*) FROM ticket_attachments WHERE uploaded_by=?) + " +
                "(SELECT COUNT(*) FROM audit_logs WHERE user_id=?) AS total_refs";
        try (Connection connection = Database.getConnection()) {
            try (PreparedStatement check = connection.prepareStatement(references)) {
                for (int i = 1; i <= 9; i++) check.setInt(i, id);
                try (ResultSet result = check.executeQuery()) {
                    if (result.next() && result.getLong(1) > 0) return false;
                }
            }
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM users WHERE user_id=?")) {
                delete.setInt(1, id);
                return delete.executeUpdate() == 1;
            }
        }
    }

    public void deactivate(int id) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET status='INACTIVE',updated_at=SYSDATETIME() WHERE user_id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public Optional<String> createResetToken(String email, String rawToken) throws SQLException {
        String find = "SELECT user_id FROM users WHERE email=? AND status='ACTIVE'";
        try (Connection connection = Database.getConnection();
             PreparedStatement query = connection.prepareStatement(find)) {
            query.setString(1, email);
            try (ResultSet result = query.executeQuery()) {
                if (!result.next()) return Optional.empty();
                int userId = result.getInt(1);
                try (PreparedStatement clear = connection.prepareStatement(
                        "DELETE FROM password_reset_tokens WHERE user_id=? OR expires_at<SYSDATETIME()")) {
                    clear.setInt(1, userId);
                    clear.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO password_reset_tokens(user_id,token_hash,expires_at) VALUES(?,?,DATEADD(MINUTE,15,SYSDATETIME()))")) {
                    insert.setInt(1, userId);
                    insert.setString(2, PasswordUtil.hashToken(rawToken));
                    insert.executeUpdate();
                }
                return Optional.of(rawToken);
            }
        }
    }

    public boolean resetPassword(String rawToken, String newPassword) throws SQLException {
        String select = "SELECT token_id,user_id FROM password_reset_tokens " +
                "WHERE token_hash=? AND used=0 AND expires_at>SYSDATETIME()";
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement query = connection.prepareStatement(select)) {
                query.setString(1, PasswordUtil.hashToken(rawToken));
                try (ResultSet result = query.executeQuery()) {
                    if (!result.next()) {
                        connection.rollback();
                        return false;
                    }
                    int tokenId = result.getInt(1);
                    int userId = result.getInt(2);
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE users SET password_hash=?,updated_at=SYSDATETIME() WHERE user_id=?")) {
                        update.setString(1, PasswordUtil.hash(newPassword));
                        update.setInt(2, userId);
                        update.executeUpdate();
                    }
                    try (PreparedStatement used = connection.prepareStatement(
                            "UPDATE password_reset_tokens SET used=1 WHERE token_id=?")) {
                        used.setInt(1, tokenId);
                        used.executeUpdate();
                    }
                    connection.commit();
                    return true;
                }
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private User map(ResultSet result) throws SQLException {
        return new User(result.getInt("user_id"), result.getString("full_name"), result.getString("email"),
                result.getString("username"), result.getString("phone"), result.getString("role"),
                result.getString("status"), result.getTimestamp("created_at").toLocalDateTime());
    }
}
