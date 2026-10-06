package com.group49.support.dao;
import com.group49.support.model.User;
import com.group49.support.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
public final class CustomerFeedbackStrategy implements FeedbackWorkflowStrategy {
 private final FeedbackDao feedback=new FeedbackDao();
    public void execute(HttpServletRequest req, User user, String action) throws Exception {
        if(!user.isCustomer()) throw new SecurityException("Customer required");
        if(!java.util.Set.of("create","update","delete").contains(action)) throw new IllegalArgumentException("Invalid feedback action");
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

}
