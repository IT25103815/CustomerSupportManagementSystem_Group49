package com.group49.support.dao;
import java.sql.SQLException; import java.util.List; import com.group49.support.model.Faq;
public final class EditorFaqStrategy implements FaqVisibilityStrategy {
 public List<Faq> list(FaqDao dao,String search) throws SQLException { return dao.list(search,true); }
}
