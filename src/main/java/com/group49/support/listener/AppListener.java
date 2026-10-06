package com.group49.support.listener;

import com.group49.support.config.Database;
import jakarta.servlet.ServletContextEvent;import jakarta.servlet.ServletContextListener;import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppListener implements ServletContextListener {
    @Override public void contextInitialized(ServletContextEvent event){
        try(var ignored=Database.getConnection()){event.getServletContext().setAttribute("databaseOnline",true);}
        catch(Exception exception){event.getServletContext().setAttribute("databaseOnline",false);event.getServletContext().log("Database connection is not available. Run the SQL script and check database.properties.",exception);}
    }
}
