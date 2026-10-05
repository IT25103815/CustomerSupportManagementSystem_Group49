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
        return com.group49.support.util.AccessPolicyFactory.forUser(user).canEditHelp();
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
            com.group49.support.dao.FaqVisibilityStrategy strategy = canManage(user)
                ? new com.group49.support.dao.EditorFaqStrategy() : new com.group49.support.dao.PublishedFaqStrategy();
            req.setAttribute("faqs", strategy.list(faqs,WebUtil.value(req.getParameter("q"))));
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
                if (categoryId <= 0 || question.length() < 8 || question.length() > 300 || answer.length() < 20 || answer.length() > 10000) {
                    WebUtil.flash(req, "error", "Choose a category and enter a clear question and answer.");
                    req.setAttribute("formError","Choose a category, a question of 8–300 characters and an answer of 20–10000 characters.");
                    int editId=WebUtil.parseInt(req.getParameter("id"),0);
                    if(editId>0) req.setAttribute("faq",faqs.find(editId).orElse(null));
                    req.setAttribute("categories",faqs.categories());
                    req.getRequestDispatcher("/WEB-INF/views/faq/form.jsp").forward(req,res); return;
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
