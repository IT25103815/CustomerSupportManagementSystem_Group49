package com.group49.support.util;
import com.group49.support.model.Ticket; import jakarta.servlet.http.HttpServletResponse;
import java.io.*; import java.util.List;
/** Print-friendly HTML. Browser Print to PDF is not a server PDF export. */
public final class HtmlReportStrategy implements ReportOutputStrategy {
 public void write(HttpServletResponse response,List<Ticket> tickets) throws IOException {
  response.setContentType("text/html;charset=UTF-8");
  var out=response.getWriter();
  out.println("<!doctype html><html lang='en'><meta charset='UTF-8'><meta name='viewport' content='width=device-width'><title>Helpify ticket report</title><style>body{font:16px Arial;color:#0f172a;margin:32px}table{border-collapse:collapse;width:100%}th,td{padding:12px;border:1px solid #cbd5e1;text-align:left}@media print{button{display:none}}</style><h1>Helpify ticket report</h1><p>Use your browser’s Print command to print or save as PDF.</p><table><thead><tr><th>Ticket</th><th>Customer</th><th>Subject</th><th>Priority</th><th>Status</th></tr></thead><tbody>");
  for(var ticket:tickets) out.println("<tr><td>"+escape(ticket.number())+"</td><td>"+escape(ticket.customerName())+"</td><td>"+escape(ticket.subject())+"</td><td>"+escape(ticket.priority())+"</td><td>"+escape(ticket.status())+"</td></tr>");
  out.println("</tbody></table></html>");
 }
 private String escape(String text){return text==null?"":text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
}
