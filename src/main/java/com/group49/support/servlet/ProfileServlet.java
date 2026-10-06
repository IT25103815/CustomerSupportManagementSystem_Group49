package com.group49.support.servlet;

import com.group49.support.dao.UserDao;
import com.group49.support.model.User;
import com.group49.support.util.PasswordUtil;
import com.group49.support.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {
    private final UserDao users = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        User current = WebUtil.currentUser(req);
        String action = WebUtil.value(req.getParameter("action"));
        try {
            if ("password".equals(action)) {
                changePassword(req, current);
            } else {
                updateProfile(req, current);
            }
            res.sendRedirect(req.getContextPath() + "/profile");
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }

    private void updateProfile(HttpServletRequest req, User current) throws Exception {
        String name = WebUtil.value(req.getParameter("name"));
        String email = WebUtil.value(req.getParameter("email"));
        String phone = WebUtil.value(req.getParameter("phone"));
        if (name.length() < 3 || name.length()>120) {
            WebUtil.flash(req, "error", "Full name must contain at least 3 characters.");
            return;
        }
        if (email.length()>160 || !WebUtil.validEmail(email)) {
            WebUtil.flash(req, "error", "Enter a valid email address.");
            return;
        }
        if (!WebUtil.validPhone(phone)) {
            WebUtil.flash(req, "error", "Enter a valid phone number or leave it blank.");
            return;
        }
        if (users.emailUsedByAnother(email, current.id())) {
            WebUtil.flash(req, "error", "That email address is already used by another account.");
            return;
        }
        users.updateProfile(current.id(), name, email, phone);
        User refreshed = users.findById(current.id()).orElseThrow();
        req.getSession().setAttribute("currentUser", refreshed);
        WebUtil.flash(req, "success", "Profile updated.");
    }

    private void changePassword(HttpServletRequest req, User current) throws Exception {
        String oldPassword = req.getParameter("currentPassword");
        String newPassword = req.getParameter("newPassword");
        String confirm = req.getParameter("confirmPassword");
        if (!users.verifyPassword(current.id(), oldPassword == null ? "" : oldPassword)) {
            WebUtil.flash(req, "error", "Current password is incorrect.");
            return;
        }
        if (!PasswordUtil.isStrong(newPassword)) {
            WebUtil.flash(req, "error", PasswordUtil.requirements());
            return;
        }
        if (!newPassword.equals(confirm)) {
            WebUtil.flash(req, "error", "New password and confirmation do not match.");
            return;
        }
        users.updatePassword(current.id(), newPassword);
        WebUtil.flash(req, "success", "Password changed successfully.");
    }
}
