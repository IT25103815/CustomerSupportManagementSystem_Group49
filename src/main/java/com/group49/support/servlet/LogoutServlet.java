package com.group49.support.servlet;
import jakarta.servlet.annotation.WebServlet;import jakarta.servlet.http.*;import java.io.IOException;
@WebServlet("/logout") public class LogoutServlet extends HttpServlet{ @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws IOException{req.getSession().invalidate();res.sendRedirect(req.getContextPath()+"/login?logout=1");}}
