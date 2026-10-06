package com.group49.support.servlet;

import com.group49.support.dao.UserDao;
import com.group49.support.util.PasswordUtil;
import com.group49.support.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {
    private final UserDao users = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setAttribute("token", WebUtil.value(req.getParameter("token")));
        req.getRequestDispatcher("/WEB-INF/views/reset-password.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirm");
        String token = WebUtil.value(req.getParameter("token"));
        if (!PasswordUtil.isStrong(password) || password == null || !password.equals(confirm)) {
            req.setAttribute("error", PasswordUtil.requirements() + " Both password fields must match.");
            req.setAttribute("token", token);
            req.getRequestDispatcher("/WEB-INF/views/reset-password.jsp").forward(req, res);
            return;
        }
        try {
            if (users.resetPassword(token, password)) {
                WebUtil.flash(req, "success", "Password updated. You can now sign in.");
                res.sendRedirect(req.getContextPath() + "/login");
            } else {
                req.setAttribute("error", "This reset link is invalid, expired or already used.");
                req.setAttribute("token", token);
                req.getRequestDispatcher("/WEB-INF/views/reset-password.jsp").forward(req, res);
            }
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }
}
