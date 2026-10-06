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
            com.group49.support.dao.FeedbackWorkflowStrategy strategy;
            if(user.isCustomer()) strategy=new com.group49.support.dao.CustomerFeedbackStrategy();
            else if(canManage(user)) strategy=new com.group49.support.dao.StaffFeedbackStrategy();
            else { res.sendError(403); return; }
            strategy.execute(req,user,action);
        } catch (Exception exception) {
            WebUtil.flash(req, "error", "Feedback could not be saved. Check the ticket status and entered values.");
        }
        if("error".equals(req.getSession().getAttribute("flashType")) || "warning".equals(req.getSession().getAttribute("flashType"))) {
            req.setAttribute("feedbackFormError",true); doGet(req,res); return;
        }
        res.sendRedirect(req.getContextPath() + "/feedback");
    }

}
