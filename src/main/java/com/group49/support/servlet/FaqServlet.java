package com.group49.support.servlet;

import com.group49.support.dao.FaqDao;
import com.group49.support.model.User;
import com.group49.support.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/faqs")
public class FaqServlet extends HttpServlet {
    private final FaqDao faqs = new FaqDao();

    private boolean canManage(User user) {
        return user != null && user.hasRole("CUSTOMER_RELATIONS_OFFICER", "CUSTOMER_SUPPORT_MANAGER");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        String action = WebUtil.value(req.getParameter("action"));
        try {
            if (("new".equals(action) || "edit".equals(action)) && !canManage(user)) {
                res.sendError(403); return;
            }
            if ("new".equals(action) || "edit".equals(action)) {
                if ("edit".equals(action)) {
                    var faq = faqs.find(WebUtil.parseInt(req.getParameter("id"), 0)).orElse(null);
                    if (faq == null) { res.sendError(404); return; }
                    req.setAttribute("faq", faq);
                }
                req.setAttribute("categories", faqs.categories());
                req.getRequestDispatcher("/WEB-INF/views/faq/form.jsp").forward(req, res);
                return;
            }
            req.setAttribute("faqs", faqs.list(WebUtil.value(req.getParameter("q")), canManage(user)));
            req.setAttribute("canManage", canManage(user));
            req.getRequestDispatcher("/WEB-INF/views/faq/list.jsp").forward(req, res);
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        if (!canManage(user)) { res.sendError(403); return; }
        String action = WebUtil.value(req.getParameter("action"));
        try {
            if ("delete".equals(action)) {
                faqs.delete(WebUtil.parseInt(req.getParameter("id"), 0));
                WebUtil.flash(req, "success", "FAQ deleted.");
            } else {
                int categoryId = WebUtil.parseInt(req.getParameter("categoryId"), 0);
                String question = WebUtil.value(req.getParameter("question"));
                String answer = WebUtil.value(req.getParameter("answer"));
                if (categoryId <= 0 || question.length() < 8 || question.length() > 300 || answer.length() < 20) {
                    WebUtil.flash(req, "error", "Choose a category and enter a clear question and answer.");
                    res.sendRedirect(req.getContextPath() + "/faqs");
                    return;
                }
                int rawId = WebUtil.parseInt(req.getParameter("id"), 0);
                Integer id = rawId == 0 ? null : rawId;
                faqs.save(id, categoryId, question, answer, "on".equals(req.getParameter("published")), user.id());
                WebUtil.flash(req, "success", id == null ? "FAQ created." : "FAQ updated.");
            }
            res.sendRedirect(req.getContextPath() + "/faqs");
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }
}
