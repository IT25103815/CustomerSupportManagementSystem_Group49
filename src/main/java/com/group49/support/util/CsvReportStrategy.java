package com.group49.support.util;
import com.group49.support.model.Ticket; import jakarta.servlet.http.HttpServletResponse;
import java.io.*; import java.time.LocalDate;
public final class CsvReportStrategy implements ReportOutputStrategy {
    public void write(HttpServletResponse res, java.util.List<Ticket> list) throws IOException {
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
        return value == null ? "" : ((value.stripLeading().matches("^[=+@\\-].*") ? "\'" : "") + value).replace("\"", "\"\"").replace("\r", " ").replace("\n", " ");
    }
}
