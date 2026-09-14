package com.group49.support.dao;

import com.group49.support.config.Database;
import com.group49.support.model.SavedReport;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReportDao {
    public List<SavedReport> list() throws SQLException {
        String sql = "SELECT r.*,u.full_name creator_name FROM saved_reports r " +
                "JOIN users u ON u.user_id=r.created_by ORDER BY r.updated_at DESC,r.created_at DESC";
        List<SavedReport> reports = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) reports.add(map(result));
        }
        return reports;
    }

    public Optional<SavedReport> find(int id) throws SQLException {
        String sql = "SELECT r.*,u.full_name creator_name FROM saved_reports r " +
                "JOIN users u ON u.user_id=r.created_by WHERE r.report_id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    public int create(String name, String reportType, String filters, int createdBy) throws SQLException {
        String sql = "INSERT INTO saved_reports(name,report_type,filters,created_by) OUTPUT INSERTED.report_id VALUES(?,?,?,?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, reportType);
            statement.setString(3, filters);
            statement.setInt(4, createdBy);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getInt(1);
            }
        }
        throw new SQLException("Report was not saved");
    }

    public void update(int id, String name, String reportType, String filters) throws SQLException {
        String sql = "UPDATE saved_reports SET name=?,report_type=?,filters=?,updated_at=SYSDATETIME() WHERE report_id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, reportType);
            statement.setString(3, filters);
            statement.setInt(4, id);
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM saved_reports WHERE report_id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private SavedReport map(ResultSet result) throws SQLException {
        Timestamp updated = result.getTimestamp("updated_at");
        return new SavedReport(result.getInt("report_id"), result.getString("name"),
                result.getString("report_type"), result.getString("filters"), result.getInt("created_by"),
                result.getString("creator_name"), result.getTimestamp("created_at").toLocalDateTime(),
                updated == null ? result.getTimestamp("created_at").toLocalDateTime() : updated.toLocalDateTime());
    }
}
