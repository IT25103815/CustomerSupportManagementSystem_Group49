package com.group49.support.dao;

import com.group49.support.config.Database;
import com.group49.support.model.Faq;
import java.sql.*;import java.util.*;

public class FaqDao {
    public List<Faq> list(String search,boolean includeDrafts)throws SQLException{
        String sql="SELECT f.*,c.name category_name,u.full_name author_name FROM faqs f LEFT JOIN faq_categories c ON c.category_id=f.category_id JOIN users u ON u.user_id=f.created_by WHERE (?='' OR f.question LIKE ? OR f.answer LIKE ?)"+(includeDrafts?"":" AND f.published=1")+" ORDER BY c.name,f.question";
        List<Faq> items=new ArrayList<>();try(Connection c=Database.getConnection();PreparedStatement s=c.prepareStatement(sql)){String like="%"+search+"%";s.setString(1,search);s.setString(2,like);s.setString(3,like);try(ResultSet r=s.executeQuery()){while(r.next())items.add(map(r));}}return items;
    }
    public Optional<Faq> find(int id)throws SQLException{String sql="SELECT f.*,c.name category_name,u.full_name author_name FROM faqs f LEFT JOIN faq_categories c ON c.category_id=f.category_id JOIN users u ON u.user_id=f.created_by WHERE faq_id=?";try(Connection c=Database.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,id);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}}
    public void save(Integer id,int category,String question,String answer,boolean published,int userId)throws SQLException{String sql=id==null?"INSERT INTO faqs(category_id,question,answer,published,created_by) VALUES(?,?,?,?,?)":"UPDATE faqs SET category_id=?,question=?,answer=?,published=?,updated_at=SYSDATETIME() WHERE faq_id=?";try(Connection c=Database.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,category);s.setString(2,question);s.setString(3,answer);s.setBoolean(4,published);s.setInt(5,id==null?userId:id);s.executeUpdate();}}
    public void delete(int id)throws SQLException{try(Connection c=Database.getConnection();PreparedStatement s=c.prepareStatement("DELETE FROM faqs WHERE faq_id=?")){s.setInt(1,id);s.executeUpdate();}}
    public List<Map<String,Object>> categories()throws SQLException{List<Map<String,Object>> list=new ArrayList<>();try(Connection c=Database.getConnection();PreparedStatement s=c.prepareStatement("SELECT category_id,name FROM faq_categories ORDER BY name");ResultSet r=s.executeQuery()){while(r.next()){Map<String,Object> m=new HashMap<>();m.put("id",r.getInt(1));m.put("name",r.getString(2));list.add(m);}}return list;}
    private Faq map(ResultSet r)throws SQLException{return new Faq(r.getInt("faq_id"),r.getInt("category_id"),r.getString("category_name"),r.getString("question"),r.getString("answer"),r.getBoolean("published"),r.getString("author_name"),r.getTimestamp("updated_at").toLocalDateTime());}
}
