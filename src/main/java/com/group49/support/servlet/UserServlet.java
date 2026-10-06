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
import java.util.Set;

@WebServlet("/users")
public class UserServlet extends HttpServlet {
    private static final Set<String> ROLES = Set.of("CUSTOMER", "CUSTOMER_RELATIONS_OFFICER",
            "SENIOR_CUSTOMER_SERVICE_OFFICER", "OPERATIONS_EXECUTIVE", "CUSTOMER_SUPPORT_MANAGER",
            "IT_SUPPORT_COORDINATOR", "QUALITY_ASSURANCE_SUPERVISOR");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE", "LOCKED");
    private final UserDao users = new UserDao();

    private boolean allowed(User user) {
        return com.group49.support.util.AccessPolicyFactory.forUser(user).canManageUsers();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        User current = WebUtil.currentUser(req);
        if (!allowed(current)) { res.sendError(403); return; }
        try {
            if ("new".equals(WebUtil.value(req.getParameter("action")))) {
                req.getRequestDispatcher("/WEB-INF/views/users/form.jsp").forward(req, res);
                return;
            }
            req.setAttribute("users", users.list(WebUtil.value(req.getParameter("q"))));
            req.getRequestDispatcher("/WEB-INF/views/users/list.jsp").forward(req, res);
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        User current = WebUtil.currentUser(req);
        if (!allowed(current)) { res.sendError(403); return; }
        String action = WebUtil.value(req.getParameter("action"));
        try {
            if ("create".equals(action)) create(req);
            else if ("delete".equals(action)) delete(req, current);
            else updateAccess(req, current);
        } catch (Exception exception) {
            WebUtil.flash(req, "error", "User changes could not be saved. Check duplicate values and required fields.");
        }
        if("create".equals(action) && "error".equals(req.getSession().getAttribute("flashType"))) {
            req.setAttribute("userFormError",true); req.getRequestDispatcher("/WEB-INF/views/users/form.jsp").forward(req,res); return;
        }
        res.sendRedirect(req.getContextPath() + "/users");
    }

    private void create(HttpServletRequest req) throws Exception {
        String name = WebUtil.value(req.getParameter("name"));
        String email = WebUtil.value(req.getParameter("email"));
        String username = WebUtil.value(req.getParameter("username"));
        String phone = WebUtil.value(req.getParameter("phone"));
        String password = req.getParameter("password");
        String role = WebUtil.value(req.getParameter("role"));
        String status = WebUtil.value(req.getParameter("status"));
        if (name.length() < 3 || name.length() > 120 || email.length() > 160 || username.length() > 80 || !WebUtil.validEmail(email) || !WebUtil.validPhone(phone) || username.length() < 4) {
            WebUtil.flash(req, "error", "Enter a valid name, email, username and phone number.");
            return;
        }
        if (!PasswordUtil.isStrong(password)) {
            WebUtil.flash(req, "error", PasswordUtil.requirements());
            return;
        }
        if (!ROLES.contains(role) || !STATUSES.contains(status)) {
            WebUtil.flash(req, "error", "Invalid role or account status.");
            return;
        }
        if (users.exists(email, username)) {
            WebUtil.flash(req, "error", "Email address or username already exists.");
            return;
        }
        users.create(name, email, username, phone, password, role, status);
        WebUtil.flash(req, "success", "User account created.");
    }

    private void updateAccess(HttpServletRequest req, User current) throws Exception {
        int id = WebUtil.parseInt(req.getParameter("id"), 0);
        if (id == current.id()) {
            WebUtil.flash(req, "warning", "You cannot change your own role or status here.");
            return;
        }
        String role = WebUtil.value(req.getParameter("role"));
        String status = WebUtil.value(req.getParameter("status"));
        if (!ROLES.contains(role) || !STATUSES.contains(status)) {
            WebUtil.flash(req, "error", "Invalid role or account status.");
            return;
        }
        users.updateStatusAndRole(id, role, status);
        WebUtil.flash(req, "success", "Account access updated.");
    }

    private void delete(HttpServletRequest req, User current) throws Exception {
        int id = WebUtil.parseInt(req.getParameter("id"), 0);
        if (id <= 0 || id == current.id()) {
            WebUtil.flash(req, "warning", "You cannot delete the current account.");
            return;
        }
        if (users.deleteIfUnused(id)) {
            WebUtil.flash(req, "success", "Unused user account deleted.");
        } else {
            users.deactivate(id);
            WebUtil.flash(req, "warning", "This account has linked records, so it was safely deactivated instead of deleted.");
        }
    }
}
