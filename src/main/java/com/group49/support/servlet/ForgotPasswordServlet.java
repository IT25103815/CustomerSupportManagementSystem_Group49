package com.group49.support.servlet;

import com.group49.support.dao.UserDao;
import com.group49.support.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {
    private final UserDao users = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/forgot-password.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String email = WebUtil.value(req.getParameter("email"));
        if (!WebUtil.validEmail(email)) {
            req.setAttribute("error", "Enter a valid email address.");
            req.getRequestDispatcher("/WEB-INF/views/forgot-password.jsp").forward(req, res);
            return;
        }
        try {
            String token = UUID.randomUUID().toString();
            var created = users.createResetToken(email, token);
            req.setAttribute("message", "If an active account matches that email, a 15-minute reset link has been created.");
            if (created.isPresent()) req.setAttribute("resetLink", req.getContextPath() + "/reset-password?token=" + created.get());
            req.getRequestDispatcher("/WEB-INF/views/forgot-password.jsp").forward(req, res);
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }
}
