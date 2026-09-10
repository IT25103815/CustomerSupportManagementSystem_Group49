package com.group49.support.dao;

import com.group49.support.config.Database;
import com.group49.support.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class TicketDao {
    private static final String SELECT = "SELECT t.*,c.name category_name,u.full_name customer_name,a.full_name assignee_name FROM tickets t JOIN ticket_categories c ON c.category_id=t.category_id JOIN users u ON u.user_id=t.customer_id LEFT JOIN users a ON a.user_id=t.assigned_to ";

    public List<Ticket> list(User user,String search,String status)throws SQLException{
        StringBuilder sql=new StringBuilder(SELECT).append(" WHERE (?='' OR t.ticket_number LIKE ? OR t.subject LIKE ?) AND (?='' OR t.status=?) ");
        if(user.isCustomer())sql.append("AND t.customer_id=? ");
        else if(user.hasRole("SENIOR_CUSTOMER_SERVICE_OFFICER"))sql.append("AND (t.assigned_to=? OR t.assigned_to IS NULL) ");
        sql.append("ORDER BY CASE t.priority WHEN 'URGENT' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END,t.updated_at DESC");
        List<Ticket> items=new ArrayList<>();String like="%"+search+"%";
        try(Connection c=Database.getConnection();PreparedStatement s=c.prepareStatement(sql.toString())){
            s.setString(1,search);s.setString(2,like);s.setString(3,like);s.setString(4,status);s.setString(5,status);if(user.isCustomer()||user.hasRole("SENIOR_CUSTOMER_SERVICE_OFFICER"))s.setInt(6,user.id());
            try(ResultSet r=s.executeQuery()){while(r.next())items.add(map(r));}
        }return items;
    }

    public Optional<Ticket> find(int id,User user)throws SQLException{
        String sql=SELECT+" WHERE t.ticket_id=?"+(user.isCustomer()?" AND t.customer_id=?":"");
        try(Connection c=Database.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);if(user.isCustomer())s.setInt(2,user.id());try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}
    }

    public int create(int customerId,int categoryId,String subject,String description,String priority)throws SQLException{
        try(Connection c=Database.getConnection()){
            c.setAutoCommit(false);
            try{
                int id;
                try(PreparedStatement s=c.prepareStatement("INSERT INTO tickets(ticket_number,customer_id,category_id,subject,description,priority) OUTPUT INSERTED.ticket_id VALUES('PENDING',?,?,?,?,?)")){
                    s.setInt(1,customerId);s.setInt(2,categoryId);s.setString(3,subject);s.setString(4,description);s.setString(5,priority);try(ResultSet r=s.executeQuery()){r.next();id=r.getInt(1);}
                }
                String number="TKT-"+LocalDateTime.now().getYear()+"-"+String.format("%05d",id);
                try(PreparedStatement s=c.prepareStatement("UPDATE tickets SET ticket_number=? WHERE ticket_id=?")){s.setString(1,number);s.setInt(2,id);s.executeUpdate();}
                try(PreparedStatement s=c.prepareStatement("INSERT INTO ticket_status_history(ticket_id,new_status,changed_by,note) VALUES(?,'OPEN',?,'Ticket created')")){s.setInt(1,id);s.setInt(2,customerId);s.executeUpdate();}
                c.commit();return id;
            }catch(SQLException e){c.rollback();throw e;}
        }
    }

    public void update(int id,String status,String priority,Integer assignedTo,String note,int changedBy)throws SQLException{
        try(Connection c=Database.getConnection()){
            c.setAutoCommit(false);try{
                String old;
                try(PreparedStatement s=c.prepareStatement("SELECT status FROM tickets WHERE ticket_id=?")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Ticket not found");old=r.getString(1);}}
                try(PreparedStatement s=c.prepareStatement("UPDATE tickets SET status=?,priority=?,assigned_to=?,updated_at=SYSDATETIME(),resolved_at=CASE WHEN ?='RESOLVED' THEN SYSDATETIME() ELSE resolved_at END,closed_at=CASE WHEN ?='CLOSED' THEN SYSDATETIME() ELSE closed_at END WHERE ticket_id=?")){
                    s.setString(1,status);s.setString(2,priority);if(assignedTo==null)s.setNull(3,Types.INTEGER);else s.setInt(3,assignedTo);s.setString(4,status);s.setString(5,status);s.setInt(6,id);s.executeUpdate();
                }
                try(PreparedStatement s=c.prepareStatement("INSERT INTO ticket_status_history(ticket_id,old_status,new_status,changed_by,note) VALUES(?,?,?,?,?)")){s.setInt(1,id);s.setString(2,old);s.setString(3,status);s.setInt(4,changedBy);s.setString(5,note);s.executeUpdate();}
                c.commit();
            }catch(SQLException e){c.rollback();throw e;}
        }
    }

    public void addMessage(int ticketId,int senderId,String message,boolean internal)throws SQLException{
        try(Connection c=Database.getConnection();PreparedStatement s=c.prepareStatement("INSERT INTO ticket_messages(ticket_id,sender_id,message,internal_note) VALUES(?,?,?,?)")){s.setInt(1,ticketId);s.setInt(2,senderId);s.setString(3,message);s.setBoolean(4,internal);s.executeUpdate();}
    }

    public List<TicketMessage> messages(int ticketId,boolean includeInternal)throws SQLException{
        List<TicketMessage> items=new ArrayList<>();String sql="SELECT m.*,u.full_name sender_name,u.role sender_role FROM ticket_messages m JOIN users u ON u.user_id=m.sender_id WHERE ticket_id=?"+(includeInternal?"":" AND internal_note=0")+" ORDER BY sent_at";
        try(Connection c=Database.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,ticketId);try(ResultSet r=s.executeQuery()){while(r.next())items.add(new TicketMessage(r.getInt("message_id"),r.getInt("ticket_id"),r.getInt("sender_id"),r.getString("sender_name"),r.getString("sender_role"),r.getString("message"),r.getBoolean("internal_note"),r.getBoolean("is_read"),r.getTimestamp("sent_at").toLocalDateTime()));}}return items;
    }

    public List<Map<String,Object>> history(int ticketId)throws SQLException{
        List<Map<String,Object>> items=new ArrayList<>();String sql="SELECT h.*,u.full_name FROM ticket_status_history h JOIN users u ON u.user_id=h.changed_by WHERE ticket_id=? ORDER BY changed_at DESC";
        try(Connection c=Database.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,ticketId);try(ResultSet r=s.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("oldStatus",r.getString("old_status"));m.put("newStatus",r.getString("new_status"));m.put("name",r.getString("full_name"));m.put("note",r.getString("note"));m.put("date",r.getTimestamp("changed_at").toLocalDateTime());items.add(m);}}}return items;
    }

    public DashboardStats stats(User user,long unread)throws SQLException{
        String where=user.isCustomer()?" WHERE customer_id=?":"";Map<String,Long> statuses=new LinkedHashMap<>();Map<String,Long> priorities=new LinkedHashMap<>();
        try(Connection c=Database.getConnection()){
            try(PreparedStatement s=c.prepareStatement("SELECT status,COUNT(*) total FROM tickets"+where+" GROUP BY status")){if(user.isCustomer())s.setInt(1,user.id());try(ResultSet r=s.executeQuery()){while(r.next())statuses.put(r.getString(1),r.getLong(2));}}
            try(PreparedStatement s=c.prepareStatement("SELECT priority,COUNT(*) total FROM tickets"+where+" GROUP BY priority")){if(user.isCustomer())s.setInt(1,user.id());try(ResultSet r=s.executeQuery()){while(r.next())priorities.put(r.getString(1),r.getLong(2));}}
            double satisfaction=0;try(PreparedStatement s=c.prepareStatement("SELECT COALESCE(AVG(CAST(rating AS FLOAT)),0) FROM feedback"+(user.isCustomer()?" WHERE customer_id=?":""))){if(user.isCustomer())s.setInt(1,user.id());try(ResultSet r=s.executeQuery()){if(r.next())satisfaction=r.getDouble(1);}}
            long total=statuses.values().stream().mapToLong(Long::longValue).sum();return new DashboardStats(total,statuses.getOrDefault("OPEN",0L),statuses.getOrDefault("IN_PROGRESS",0L),statuses.getOrDefault("ESCALATED",0L),statuses.getOrDefault("RESOLVED",0L),statuses.getOrDefault("CLOSED",0L),unread,satisfaction,statuses,priorities);
        }
    }

    public List<Map<String,Object>> categories()throws SQLException{return simpleOptions("SELECT category_id id,name FROM ticket_categories ORDER BY name");}
    private List<Map<String,Object>> simpleOptions(String sql)throws SQLException{List<Map<String,Object>> list=new ArrayList<>();try(Connection c=Database.getConnection();PreparedStatement s=c.prepareStatement(sql);ResultSet r=s.executeQuery()){while(r.next()){Map<String,Object> m=new HashMap<>();m.put("id",r.getInt("id"));m.put("name",r.getString("name"));list.add(m);}}return list;}
    private Ticket map(ResultSet r)throws SQLException{Integer assigned=(Integer)r.getObject("assigned_to");return new Ticket(r.getInt("ticket_id"),r.getString("ticket_number"),r.getInt("customer_id"),r.getString("customer_name"),r.getInt("category_id"),r.getString("category_name"),assigned,r.getString("assignee_name"),r.getString("subject"),r.getString("description"),r.getString("priority"),r.getString("status"),r.getTimestamp("created_at").toLocalDateTime(),r.getTimestamp("updated_at").toLocalDateTime());}
}
