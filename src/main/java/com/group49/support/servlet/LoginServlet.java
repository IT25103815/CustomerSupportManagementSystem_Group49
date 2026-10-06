package com.group49.support.servlet;

import com.group49.support.dao.UserDao;import com.group49.support.model.User;import com.group49.support.util.WebUtil;
import jakarta.servlet.*;import jakarta.servlet.annotation.WebServlet;import jakarta.servlet.http.*;import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet{
    private final UserDao users=new UserDao();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{if(WebUtil.currentUser(req)!=null){res.sendRedirect(req.getContextPath()+"/dashboard");return;}req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req,res);}
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{String login=WebUtil.value(req.getParameter("login"));String password=req.getParameter("password");try{var result=users.authenticate(login,password==null?"":password);if(result.isPresent()){HttpSession old=req.getSession(false);if(old!=null)old.invalidate();HttpSession session=req.getSession(true);session.setAttribute("currentUser",result.get());session.setAttribute("csrfToken",java.util.UUID.randomUUID().toString());session.setMaxInactiveInterval(30*60);WebUtil.flash(req,"success","Welcome back, "+result.get().fullName()+".");res.sendRedirect(req.getContextPath()+"/dashboard");}else{req.setAttribute("error","Invalid username/email or password, or the account is inactive.");req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req,res);}}catch(Exception e){req.setAttribute("error","Database connection failed. Check SQL Server and database.properties.");req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req,res);}}
}
