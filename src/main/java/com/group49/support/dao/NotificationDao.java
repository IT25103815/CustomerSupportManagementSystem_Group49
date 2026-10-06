package com.group49.support.dao;

import com.group49.support.config.Database;
import com.group49.support.model.Notification;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDao {
    public void create(int userId, String title, String message, String link) throws SQLException {
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO notifications(user_id,title,message,link) VALUES(?,?,?,?)")) {
            statement.setInt(1,userId); statement.setString(2,title); statement.setString(3,message); statement.setString(4,link); statement.executeUpdate();
        }
    }

    public List<Notification> list(int userId) throws SQLException {
        List<Notification> items = new ArrayList<>();
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM notifications WHERE user_id=? ORDER BY created_at DESC")) {
            statement.setInt(1,userId);
            try(ResultSet result=statement.executeQuery()){ while(result.next()) items.add(map(result)); }
        }
        return items;
    }

    public long unreadCount(int userId) throws SQLException {
        try(Connection connection=Database.getConnection();PreparedStatement statement=connection.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id=? AND is_read=0")){
            statement.setInt(1,userId);try(ResultSet result=statement.executeQuery()){return result.next()?result.getLong(1):0;}
        }
    }

    public void markRead(int userId, Integer id) throws SQLException {
        String sql=id==null?"UPDATE notifications SET is_read=1 WHERE user_id=?":"UPDATE notifications SET is_read=1 WHERE user_id=? AND notification_id=?";
        try(Connection connection=Database.getConnection();PreparedStatement statement=connection.prepareStatement(sql)){
            statement.setInt(1,userId);if(id!=null)statement.setInt(2,id);statement.executeUpdate();
        }
    }

    public void delete(int userId,int id)throws SQLException{
        try(Connection connection=Database.getConnection();PreparedStatement statement=connection.prepareStatement("DELETE FROM notifications WHERE user_id=? AND notification_id=?")){
            statement.setInt(1,userId);statement.setInt(2,id);statement.executeUpdate();
        }
    }

    private Notification map(ResultSet r)throws SQLException{return new Notification(r.getInt("notification_id"),r.getInt("user_id"),r.getString("title"),r.getString("message"),r.getString("link"),r.getBoolean("is_read"),r.getTimestamp("created_at").toLocalDateTime());}
}
