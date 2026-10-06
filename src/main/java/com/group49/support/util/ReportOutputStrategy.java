package com.group49.support.util;
import jakarta.servlet.http.HttpServletResponse;
import com.group49.support.model.Ticket;
import java.io.IOException; import java.util.List;
public interface ReportOutputStrategy { void write(HttpServletResponse response,List<Ticket> tickets) throws IOException; }
