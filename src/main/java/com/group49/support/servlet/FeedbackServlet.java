package com.group49.support.servlet;

import com.group49.support.dao.FeedbackDao;
import com.group49.support.dao.TicketDao;
import com.group49.support.model.User;
import com.group49.support.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/feedback")
public class FeedbackServlet extends HttpServlet {
    private final FeedbackDao feedback = new FeedbackDao();
    private final TicketDao tickets = new TicketDao();

    private boolean canManage(User user) {
        return user.hasRole("CUSTOMER_RELATIONS_OFFICER", "CUSTOMER_SUPPORT_MANAGER");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        try {
            req.setAttribute("feedbackList", feedback.list(user));
            if (user.isCustomer()) req.setAttribute("eligibleTickets", tickets.eligibleForFeedback(user.id()));
            req.getRequestDispatcher("/WEB-INF/views/feedback/list.jsp").forward(req, res);
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        String action = WebUtil.value(req.getParameter("action"));
        try {
            if (user.isCustomer()) handleCustomer(req, user, action);
            else if (canManage(user)) handleStaff(req, action);
            else { res.sendError(403); return; }
        } catch (Exception exception) {
            WebUtil.flash(req, "error", "Feedback could not be saved. Check the ticket status and entered values.");
        }
        res.sendRedirect(req.getContextPath() + "/feedback");
    }

    private void handleCustomer(HttpServletRequest req, User user, String action) throws Exception {
        int rating = WebUtil.parseInt(req.getParameter("rating"), 0);
        String comments = WebUtil.value(req.getParameter("comments"));
        if ("delete".equals(action)) {
            if (feedback.deleteByCustomer(WebUtil.parseInt(req.getParameter("id"), 0), user.id()))
                WebUtil.flash(req, "success", "Feedback deleted.");
            else WebUtil.flash(req, "warning", "Only new feedback can be deleted.");
            return;
        }
        if (rating < 1 || rating > 5 || comments.length() > 1000) {
            WebUtil.flash(req, "error", "Choose a rating from 1 to 5 and keep comments under 1000 characters.");
            return;
        }
        if ("update".equals(action)) {
            if (feedback.updateByCustomer(WebUtil.parseInt(req.getParameter("id"), 0), user.id(), rating, comments))
                WebUtil.flash(req, "success", "Feedback updated.");
            else WebUtil.flash(req, "warning", "Only new feedback can be edited.");
        } else {
            feedback.submit(WebUtil.parseInt(req.getParameter("ticketId"), 0), user.id(), rating, comments);
            WebUtil.flash(req, "success", "Thank you for your feedback.");
        }
    }

    private void handleStaff(HttpServletRequest req, String action) throws Exception {
        int id = WebUtil.parseInt(req.getParameter("id"), 0);
        if ("remove".equals(action)) {
            feedback.removeByStaff(id);
            WebUtil.flash(req, "success", "Feedback removed from active review.");
            return;
        }
        String response = WebUtil.value(req.getParameter("response"));
        if (response.length() < 3 || response.length() > 1000) {
            WebUtil.flash(req, "error", "Enter a response between 3 and 1000 characters.");
            return;
        }
        feedback.respond(id, response, "RESPONDED");
        WebUtil.flash(req, "success", "Feedback response saved.");
    }
}
