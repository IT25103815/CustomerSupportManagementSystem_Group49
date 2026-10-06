package com.group49.support.servlet;

import com.group49.support.dao.ReportDao;
import com.group49.support.dao.TicketDao;
import com.group49.support.model.SavedReport;
import com.group49.support.model.Ticket;
import com.group49.support.model.User;
import com.group49.support.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@WebServlet("/reports")
public class ReportServlet extends HttpServlet {
    private final TicketDao tickets = new TicketDao();
    private final ReportDao reports = new ReportDao();

    private boolean allowed(User user) {
        return user.hasRole("CUSTOMER_SUPPORT_MANAGER", "OPERATIONS_EXECUTIVE", "QUALITY_ASSURANCE_SUPERVISOR");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        if (!allowed(user)) { res.sendError(403); return; }
        try {
            String q = WebUtil.value(req.getParameter("q"));
            String status = WebUtil.value(req.getParameter("status"));
            if ("load".equals(req.getParameter("action"))) {
                SavedReport saved = reports.find(WebUtil.parseInt(req.getParameter("id"), 0)).orElse(null);
                if (saved == null) { res.sendError(404); return; }
                String[] parts = saved.filters() == null ? new String[0] : saved.filters().split("\\|", -1);
                status = parts.length > 0 ? parts[0] : "";
                q = parts.length > 1 ? parts[1] : "";
            }
            var list = tickets.list(user, q, status);
            if ("csv".equals(req.getParameter("format"))) {
                writeCsv(res, list);
                return;
            }
            req.setAttribute("reportTickets", list);
            req.setAttribute("stats", tickets.stats(user, 0));
            req.setAttribute("savedReports", reports.list());
            req.setAttribute("reportQ", q);
            req.setAttribute("reportStatus", status);
            req.getRequestDispatcher("/WEB-INF/views/reports/index.jsp").forward(req, res);
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        if (!allowed(user)) { res.sendError(403); return; }
        String action = WebUtil.value(req.getParameter("action"));
        try {
            if ("delete".equals(action)) {
                reports.delete(WebUtil.parseInt(req.getParameter("id"), 0));
                WebUtil.flash(req, "success", "Saved report deleted.");
            } else {
                String name = WebUtil.value(req.getParameter("name"));
                String type = WebUtil.value(req.getParameter("reportType"));
                String q = WebUtil.value(req.getParameter("q"));
                String status = WebUtil.value(req.getParameter("status"));
                if (name.length() < 3 || name.length() > 150) {
                    WebUtil.flash(req, "error", "Report name must contain 3 to 150 characters.");
                    res.sendRedirect(req.getContextPath() + "/reports");
                    return;
                }
                if (type.isBlank()) type = "TICKET_REGISTER";
                String filters = status.replace("|", "") + "|" + q.replace("|", "");
                if ("update".equals(action)) {
                    reports.update(WebUtil.parseInt(req.getParameter("id"), 0), name, type, filters);
                    WebUtil.flash(req, "success", "Saved report updated.");
                } else {
                    reports.create(name, type, filters, user.id());
                    WebUtil.flash(req, "success", "Report saved.");
                }
            }
            res.sendRedirect(req.getContextPath() + "/reports");
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }

    private void writeCsv(HttpServletResponse res, java.util.List<Ticket> list) throws IOException {
        res.setContentType("text/csv;charset=UTF-8");
        res.setHeader("Content-Disposition", "attachment; filename=helpify-ticket-report-" + LocalDate.now() + ".csv");
        PrintWriter out = res.getWriter();
        out.println("Ticket Number,Customer,Category,Subject,Priority,Status,Assigned To,Created");
        for (Ticket ticket : list) {
            out.printf("\"%s\",\"%s\",\"%s\",\"%s\",%s,%s,\"%s\",%s%n",
                    clean(ticket.number()), clean(ticket.customerName()), clean(ticket.categoryName()),
                    clean(ticket.subject()), ticket.priority(), ticket.status(), clean(ticket.assigneeName()), ticket.createdAt());
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ");
    }
}
