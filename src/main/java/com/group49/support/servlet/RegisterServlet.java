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

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private final UserDao users = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String name = WebUtil.value(req.getParameter("name"));
        String email = WebUtil.value(req.getParameter("email"));
        String username = WebUtil.value(req.getParameter("username"));
        String phone = WebUtil.value(req.getParameter("phone"));
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirm");

        String error = null;
        if (name.length() < 3 || name.length() > 120) error = "Enter your full name (at least 3 characters).";
        else if (email.length()>160 || !WebUtil.validEmail(email)) error = "Enter a valid email address.";
        else if (!WebUtil.validPhone(phone)) error = "Enter a valid phone number or leave it blank.";
        else if (username.length() < 4 || username.length() > 80) error = "Username must contain 4 to 80 characters.";
        else if (!PasswordUtil.isStrong(password)) error = PasswordUtil.requirements();
        else if (!password.equals(confirm)) error = "Password and confirmation do not match.";

        if (error != null) {
            req.setAttribute("error", error);
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, res);
            return;
        }

        try {
            if (users.exists(email, username)) {
                req.setAttribute("error", "That email address or username already exists.");
                req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, res);
                return;
            }
            users.createCustomer(name, email, username, phone, password);
            WebUtil.flash(req, "success", "Account created. You can now sign in.");
            res.sendRedirect(req.getContextPath() + "/login");
        } catch (Exception exception) {
            req.setAttribute("error", "Registration failed. Check the database connection and try again.");
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, res);
        }
    }
}
