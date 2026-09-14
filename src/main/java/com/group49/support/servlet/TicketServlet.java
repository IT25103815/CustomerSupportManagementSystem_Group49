package com.group49.support.servlet;

import com.group49.support.dao.NotificationDao;
import com.group49.support.dao.MessageDao;
import com.group49.support.dao.TicketDao;
import com.group49.support.dao.UserDao;
import com.group49.support.model.Ticket;
import com.group49.support.model.User;
import com.group49.support.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

@WebServlet("/tickets")
public class TicketServlet extends HttpServlet {
    private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");
    private static final Set<String> STATUSES = Set.of("OPEN", "ASSIGNED", "IN_PROGRESS", "WAITING_FOR_CUSTOMER",
            "ESCALATED", "RESOLVED", "CLOSED", "CANCELLED", "REOPENED");

    private final TicketDao tickets = new TicketDao();
    private final UserDao users = new UserDao();
    private final NotificationDao notifications = new NotificationDao();
    private final MessageDao messages = new MessageDao();

    private boolean canManageTickets(User user) {
        return user.hasRole("SENIOR_CUSTOMER_SERVICE_OFFICER", "OPERATIONS_EXECUTIVE", "CUSTOMER_SUPPORT_MANAGER");
    }

    private boolean canMessage(User user) {
        return user.isCustomer() || user.hasRole("CUSTOMER_RELATIONS_OFFICER", "SENIOR_CUSTOMER_SERVICE_OFFICER",
                "OPERATIONS_EXECUTIVE", "CUSTOMER_SUPPORT_MANAGER");
    }

    private boolean customerCanEdit(Ticket ticket) {
        return ticket != null && "OPEN".equals(ticket.status()) && ticket.assignedTo() == null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        String action = WebUtil.value(req.getParameter("action"));
        try {
            if ("new".equals(action)) {
                if (!user.isCustomer()) { res.sendError(403); return; }
                req.setAttribute("categories", tickets.categories());
                view(req, res, "tickets/form.jsp");
                return;
            }
            if ("edit".equals(action)) {
                if (!user.isCustomer()) { res.sendError(403); return; }
                Ticket ticket = tickets.find(WebUtil.parseInt(req.getParameter("id"), 0), user).orElse(null);
                if (ticket == null) { res.sendError(404); return; }
                if (!customerCanEdit(ticket)) {
                    WebUtil.flash(req, "warning", "This ticket can no longer be edited because support processing has started.");
                    res.sendRedirect(req.getContextPath() + "/tickets?action=view&id=" + ticket.id());
                    return;
                }
                req.setAttribute("ticket", ticket);
                req.setAttribute("categories", tickets.categories());
                view(req, res, "tickets/form.jsp");
                return;
            }
            if ("view".equals(action)) {
                int id = WebUtil.parseInt(req.getParameter("id"), 0);
                Ticket ticket = tickets.find(id, user).orElse(null);
                if (ticket == null) { res.sendError(404); return; }
                req.setAttribute("ticket", ticket);
                req.setAttribute("messages", messages.listByTicket(id, !user.isCustomer()));
                req.setAttribute("history", tickets.history(id));
                req.setAttribute("canMessage", canMessage(user));
                req.setAttribute("customerCanEdit", user.isCustomer() && customerCanEdit(ticket));
                if (!user.isCustomer()) req.setAttribute("staff", users.supportStaff());
                view(req, res, "tickets/detail.jsp");
                return;
            }
            String search = WebUtil.value(req.getParameter("q"));
            String status = WebUtil.value(req.getParameter("status"));
            req.setAttribute("tickets", tickets.list(user, search, status));
            req.setAttribute("search", search);
            req.setAttribute("selectedStatus", status);
            view(req, res, "tickets/list.jsp");
        } catch (Exception exception) {
            throw new ServletException("Ticket operation failed", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        String action = WebUtil.value(req.getParameter("action"));
        try {
            if ("create".equals(action) && user.isCustomer()) {
                createTicket(req, res, user);
                return;
            }
            int id = WebUtil.parseInt(req.getParameter("id"), 0);
            Ticket ticket = tickets.find(id, user).orElse(null);
            if (ticket == null) { res.sendError(404); return; }

            if ("customerUpdate".equals(action) && user.isCustomer()) {
                updateCustomerTicket(req, id, user);
            } else if ("cancel".equals(action) && user.isCustomer()) {
                if (tickets.cancelByCustomer(id, user.id())) WebUtil.flash(req, "success", "Ticket cancelled.");
                else WebUtil.flash(req, "warning", "This ticket can no longer be cancelled because processing has started.");
            } else if ("delete".equals(action) && user.hasRole("CUSTOMER_SUPPORT_MANAGER")) {
                if (tickets.deleteCancelled(id)) {
                    WebUtil.flash(req, "success", "Cancelled ticket record deleted.");
                    res.sendRedirect(req.getContextPath() + "/tickets");
                    return;
                }
                WebUtil.flash(req, "warning", "Only cancelled tickets without feedback can be permanently deleted.");
            } else if ("message".equals(action) && canMessage(user)) {
                addMessage(req, ticket, user);
            } else if ("update".equals(action) && canManageTickets(user)) {
                manageTicket(req, ticket, user);
            } else {
                res.sendError(403);
                return;
            }
            res.sendRedirect(req.getContextPath() + "/tickets?action=view&id=" + id);
        } catch (Exception exception) {
            throw new ServletException("Unable to save ticket changes", exception);
        }
    }

    private void createTicket(HttpServletRequest req, HttpServletResponse res, User user) throws Exception {
        String subject = WebUtil.value(req.getParameter("subject"));
        String description = WebUtil.value(req.getParameter("description"));
        String priority = WebUtil.value(req.getParameter("priority"));
        int categoryId = WebUtil.parseInt(req.getParameter("categoryId"), 0);
        if (categoryId <= 0 || subject.length() < 5 || subject.length() > 220 ||
                description.length() < 10 || description.length() > 5000 || !PRIORITIES.contains(priority)) {
            WebUtil.flash(req, "error", "Choose a category and enter a clear subject, description and valid priority.");
            res.sendRedirect(req.getContextPath() + "/tickets?action=new");
            return;
        }
        int id = tickets.create(user.id(), categoryId, subject, description, priority);
        WebUtil.flash(req, "success", "Support ticket created successfully.");
        res.sendRedirect(req.getContextPath() + "/tickets?action=view&id=" + id);
    }

    private void updateCustomerTicket(HttpServletRequest req, int id, User user) throws Exception {
        String subject = WebUtil.value(req.getParameter("subject"));
        String description = WebUtil.value(req.getParameter("description"));
        String priority = WebUtil.value(req.getParameter("priority"));
        int categoryId = WebUtil.parseInt(req.getParameter("categoryId"), 0);
        if (categoryId <= 0 || subject.length() < 5 || subject.length() > 220 ||
                description.length() < 10 || description.length() > 5000 || !PRIORITIES.contains(priority)) {
            WebUtil.flash(req, "error", "Enter valid ticket details.");
            return;
        }
        if (tickets.updateCustomerDetails(id, user.id(), categoryId, subject, description, priority))
            WebUtil.flash(req, "success", "Ticket details updated.");
        else WebUtil.flash(req, "warning", "This ticket can no longer be edited because support processing has started.");
    }

    private void addMessage(HttpServletRequest req, Ticket ticket, User user) throws Exception {
        String message = WebUtil.value(req.getParameter("message"));
        if (message.length() < 2 || message.length() > 4000) {
            WebUtil.flash(req, "error", "Message must contain 2 to 4000 characters.");
            return;
        }
        boolean internal = !user.isCustomer() && "on".equals(req.getParameter("internal"));
        messages.create(ticket.id(), user.id(), message, internal);
        if (!internal) {
            int recipient = user.isCustomer() ? (ticket.assignedTo() == null ? 0 : ticket.assignedTo()) : ticket.customerId();
            if (recipient > 0) {
                notifications.create(recipient, "New ticket message",
                        "A new message was added to " + ticket.number(), "/tickets?action=view&id=" + ticket.id());
            }
        }
        WebUtil.flash(req, "success", internal ? "Internal note added." : "Message sent.");
    }

    private void manageTicket(HttpServletRequest req, Ticket ticket, User user) throws Exception {
        String status = WebUtil.value(req.getParameter("status"));
        String priority = WebUtil.value(req.getParameter("priority"));
        if (!STATUSES.contains(status) || !PRIORITIES.contains(priority)) {
            WebUtil.flash(req, "error", "Invalid ticket status or priority.");
            return;
        }
        Integer assigned = WebUtil.parseInt(req.getParameter("assignedTo"), 0);
        if (assigned == 0) assigned = null;
        Integer previousAssignee = ticket.assignedTo();
        tickets.update(ticket.id(), status, priority, assigned, WebUtil.value(req.getParameter("note")), user.id());

        notifications.create(ticket.customerId(), "Ticket updated",
                ticket.number() + " is now " + status.replace('_', ' '),
                "/tickets?action=view&id=" + ticket.id());

        if (assigned != null && !Objects.equals(previousAssignee, assigned)) {
            notifications.create(assigned, "New ticket assignment",
                    ticket.number() + " - " + ticket.subject() + " has been assigned to you.",
                    "/tickets?action=view&id=" + ticket.id());
        }
        WebUtil.flash(req, "success", "Ticket updated. Relevant users were notified.");
    }

    private void view(HttpServletRequest req, HttpServletResponse res, String page)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/" + page).forward(req, res);
    }
}
