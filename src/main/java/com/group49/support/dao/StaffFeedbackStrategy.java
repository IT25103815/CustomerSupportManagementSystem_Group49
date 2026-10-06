package com.group49.support.dao;
import com.group49.support.model.User;
import com.group49.support.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
public final class StaffFeedbackStrategy implements FeedbackWorkflowStrategy {
 private final FeedbackDao feedback=new FeedbackDao();
    public void execute(HttpServletRequest req, User user, String action) throws Exception {
        if(!com.group49.support.util.AccessPolicyFactory.forUser(user).canEditHelp()) throw new SecurityException("Staff permission required");
        if(!java.util.Set.of("respond","remove").contains(action)) throw new IllegalArgumentException("Invalid feedback action");
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
