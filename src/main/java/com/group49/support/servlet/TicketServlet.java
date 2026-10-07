package com.group49.support.servlet;

import com.group49.support.dao.NotificationDao;
import com.group49.support.dao.MessageDao;
import com.group49.support.dao.TicketDao;
import com.group49.support.dao.UserDao;
import com.group49.support.model.Ticket;
import com.group49.support.model.TicketAttachment;
import com.group49.support.model.User;
import com.group49.support.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@WebServlet("/tickets")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024L, maxRequestSize = 31 * 1024 * 1024L)
public class TicketServlet extends HttpServlet {
    private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");
    private static final Set<String> STATUSES = Set.of("OPEN", "ASSIGNED", "IN_PROGRESS", "WAITING_FOR_CUSTOMER",
            "ESCALATED", "RESOLVED", "CLOSED", "CANCELLED", "REOPENED");
    private final TicketDao tickets = new TicketDao();
    private final UserDao users = new UserDao();

    private final MessageDao messages = new MessageDao();

    private boolean canManageTickets(User user) {
        return user.hasRole("SENIOR_CUSTOMER_SERVICE_OFFICER", "OPERATIONS_EXECUTIVE", "CUSTOMER_SUPPORT_MANAGER");
    }

    private boolean canMessage(User user) {
        return user.isCustomer() || user.hasRole("CUSTOMER_RELATIONS_OFFICER", "SENIOR_CUSTOMER_SERVICE_OFFICER",
                "OPERATIONS_EXECUTIVE", "CUSTOMER_SUPPORT_MANAGER");
    }

    private boolean customerCanEdit(Ticket ticket) {
        return ticket != null && "OPEN".equals(ticket.status()) && ticket.assignedTo() == null;
    }

    private boolean customerCanUpload(Ticket ticket) {
        return ticket != null && !Set.of("CLOSED", "CANCELLED").contains(ticket.status());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        String action = WebUtil.value(req.getParameter("action"));
        try {
            if ("attachment".equals(action)) {
                serveAttachment(req, res, user);
                return;
            }
            if ("new".equals(action)) {
                if (!user.isCustomer()) { res.sendError(403); return; }
                req.setAttribute("categories", tickets.categories());
                view(req, res, "tickets/form.jsp");
                return;
            }
            if ("edit".equals(action)) {
                if (!user.isCustomer()) { res.sendError(403); return; }
                Ticket ticket = tickets.find(WebUtil.parseInt(req.getParameter("id"), 0), user).orElse(null);
                if (ticket == null) { res.sendError(404); return; }
                if (!customerCanEdit(ticket)) {
                    WebUtil.flash(req, "warning", "This ticket can no longer be edited because support processing has started.");
                    res.sendRedirect(req.getContextPath() + "/tickets?action=view&id=" + ticket.id());
                    return;
                }
                req.setAttribute("ticket", ticket);
                req.setAttribute("categories", tickets.categories());
                view(req, res, "tickets/form.jsp");
                return;
            }
            if ("view".equals(action)) {
                int id = WebUtil.parseInt(req.getParameter("id"), 0);
                Ticket ticket = tickets.find(id, user).orElse(null);
                if (ticket == null) { res.sendError(404); return; }
                req.setAttribute("ticket", ticket);
                req.setAttribute("messages", messages.listByTicket(id, !user.isCustomer()));
                req.setAttribute("history", tickets.history(id));
                req.setAttribute("attachments", tickets.attachments(id));
                req.setAttribute("canMessage", canMessage(user));
                req.setAttribute("customerCanEdit", user.isCustomer() && customerCanEdit(ticket));
                req.setAttribute("customerCanUpload", user.isCustomer() && customerCanUpload(ticket));
                if (!user.isCustomer()) req.setAttribute("staff", users.supportStaff());
                view(req, res, "tickets/detail.jsp");
                return;
            }
            String search = WebUtil.value(req.getParameter("q"));
            String status = WebUtil.value(req.getParameter("status"));
            if(search.length()>220 || (!status.isBlank() && !STATUSES.contains(status))) { res.sendError(400,"Invalid ticket filters"); return; }
            req.setAttribute("tickets", tickets.list(user, search, status));
            req.setAttribute("search", search);
            req.setAttribute("selectedStatus", status);
            view(req, res, "tickets/list.jsp");
        } catch (Exception exception) {
            throw new ServletException("Ticket operation failed", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        User user = WebUtil.currentUser(req);
        String action = "";
        try {
            action = WebUtil.value(req.getParameter("action"));
            if ("create".equals(action) && user.isCustomer()) {
                createTicket(req, res, user);
                return;
            }
            int id = WebUtil.parseInt(req.getParameter("id"), 0);
            if ("delete".equals(action)) {
                if (!user.hasRole("CUSTOMER_SUPPORT_MANAGER")) {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                if (!tickets.deleteTicket(id)) {
                    res.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                WebUtil.flash(req, "success", "Ticket permanently deleted.");
                res.sendRedirect(req.getContextPath() + "/tickets");
                return;
            }

            Ticket ticket = tickets.find(id, user).orElse(null);
            if (ticket == null) { res.sendError(404); return; }

            if ("uploadAttachment".equals(action) && user.isCustomer()) {
                uploadAttachment(req, ticket, user);
            } else if ("customerUpdate".equals(action) && user.isCustomer()) {
                updateCustomerTicket(req, id, user);
            } else if ("cancel".equals(action) && user.isCustomer()) {
                if (tickets.cancelByCustomer(id, user.id())) WebUtil.flash(req, "success", "Ticket cancelled.");
                else WebUtil.flash(req, "warning", "This ticket can no longer be cancelled because processing has started.");
            } else if ("message".equals(action) && canMessage(user)) {
                addMessage(req, ticket, user);
            } else if ("update".equals(action) && canManageTickets(user)) {
                manageTicket(req, ticket, user);
            } else {
                res.sendError(403);
                return;
            }
            res.sendRedirect(req.getContextPath() + "/tickets?action=view&id=" + id);
        } catch (IllegalArgumentException exception) {
            WebUtil.flash(req,"error",exception.getMessage());
            res.sendRedirect(req.getContextPath()+"/tickets");
        } catch (IllegalStateException exception) {
            WebUtil.flash(req, "error", "Upload is too large. Maximum 10 MB per file and 3 files.");
            res.sendRedirect(req.getContextPath() + "/tickets");
        } catch (Exception exception) {
            throw new ServletException("Unable to save ticket changes", exception);
        }
    }

    private void createTicket(HttpServletRequest req, HttpServletResponse res, User user) throws Exception {
        String subject = WebUtil.value(req.getParameter("subject"));
        String description = WebUtil.value(req.getParameter("description"));
        String priority = WebUtil.value(req.getParameter("priority"));
        int categoryId = WebUtil.parseInt(req.getParameter("categoryId"), 0);
        if (categoryId <= 0 || subject.length() < 5 || subject.length() > 220 ||
                description.length() < 10 || description.length() > 5000 || !PRIORITIES.contains(priority)) {
            WebUtil.flash(req, "error", "Choose a category and enter a clear subject, description and valid priority.");
            showCreateError(req,res,"Choose a category, a 5–220 character subject, a 10–5000 character description and a valid priority.");
            return;
        }
        java.util.List<com.group49.support.util.AttachmentValidator.Upload> uploads;
        try { uploads = selectedUploads(req, false); }
        catch (IllegalArgumentException | IOException e) { showCreateError(req,res,e.getMessage()+" Please select files again."); return; }
        var names = new java.util.ArrayList<String>();
        var paths = new java.util.ArrayList<Path>();
        int id;
        try {
            Files.createDirectories(uploadRoot());
            for(var upload:uploads) {
                String stored = UUID.randomUUID().toString().replace("-", "") + upload.extension();
                Path path=uploadRoot().resolve(stored); paths.add(path); names.add(stored);
                Files.write(path,upload.bytes(),java.nio.file.StandardOpenOption.CREATE_NEW);
            }
            id = tickets.create(user.id(), categoryId, subject, description, priority, uploads, names);
        } catch(Exception e) {
            for(Path path:paths) try { Files.deleteIfExists(path); } catch(IOException cleanup) { e.addSuppressed(cleanup); }
            showCreateError(req,res,"Ticket was not created. Please try again and select files again.");
            getServletContext().log("Ticket creation failed",e); return;
        }
        WebUtil.flash(req, "success", "Support ticket created successfully with " + uploads.size() + " attachment(s).");
        res.sendRedirect(req.getContextPath() + "/tickets?action=view&id=" + id);
    }

    private void updateCustomerTicket(HttpServletRequest req, int id, User user) throws Exception {
        String subject = WebUtil.value(req.getParameter("subject"));
        String description = WebUtil.value(req.getParameter("description"));
        String priority = WebUtil.value(req.getParameter("priority"));
        int categoryId = WebUtil.parseInt(req.getParameter("categoryId"), 0);
        if (categoryId <= 0 || subject.length() < 5 || subject.length() > 220 ||
                description.length() < 10 || description.length() > 5000 || !PRIORITIES.contains(priority)) {
            WebUtil.flash(req, "error", "Enter valid ticket details.");
            return;
        }
        if (tickets.updateCustomerDetails(id, user.id(), categoryId, subject, description, priority))
            WebUtil.flash(req, "success", "Ticket details updated.");
        else WebUtil.flash(req, "warning", "This ticket can no longer be edited because support processing has started.");
    }

    private void uploadAttachment(HttpServletRequest req, Ticket ticket, User user) throws Exception {
        if (!customerCanUpload(ticket)) {
            WebUtil.flash(req, "warning", "Screenshots cannot be added to a closed or cancelled ticket.");
            return;
        }

        java.util.List<com.group49.support.util.AttachmentValidator.Upload> uploads;
        try { uploads=selectedUploads(req,true); }
        catch(IllegalArgumentException | IOException e) { WebUtil.flash(req,"error",e.getMessage()); return; }
        // Later uploads are one file at a time, keeping each operation atomic.
        var upload=uploads.get(0);
        String storedName=UUID.randomUUID().toString().replace("-", "")+upload.extension();
        Path directory=uploadRoot().resolve(String.valueOf(ticket.id()));
        Files.createDirectories(directory);
        Path target=directory.resolve(storedName);
        try {
            Files.write(target,upload.bytes(),java.nio.file.StandardOpenOption.CREATE_NEW);
            tickets.createAttachment(ticket.id(),upload.name(),storedName,upload.type(),upload.bytes().length,user.id());
        } catch(Exception e) { Files.deleteIfExists(target); throw e; }
        boolean notified=true;
        if(ticket.assignedTo()!=null) notified=publish(ticket.assignedTo(),ticket.id(),"New ticket attachment","A file was attached to "+ticket.number());
        WebUtil.flash(req,notified?"success":"warning",notified?"File attached to the ticket.":"File saved, but notification could not be delivered.");
    }

    private java.util.List<com.group49.support.util.AttachmentValidator.Upload> selectedUploads(HttpServletRequest req, boolean required) throws Exception {
        var parts=new java.util.ArrayList<Part>();
        for(Part part:req.getParts()) if("attachment".equals(part.getName()) && part.getSubmittedFileName()!=null && !part.getSubmittedFileName().isBlank()) parts.add(part);
        if(parts.size()>com.group49.support.util.AttachmentValidator.MAX_FILES || (required && parts.size()!=1)) throw new IllegalArgumentException(required?"Select one file.":"Select at most 3 files.");
        var uploads=new java.util.ArrayList<com.group49.support.util.AttachmentValidator.Upload>();
        for(Part part:parts) uploads.add(com.group49.support.util.AttachmentValidator.validate(part));
        return uploads;
    }
    private void showCreateError(HttpServletRequest req,HttpServletResponse res,String error) throws Exception {
        req.setAttribute("formError",error);
        req.setAttribute("categories",tickets.categories());
        view(req,res,"tickets/form.jsp");
    }
    private boolean publish(int recipient,int ticketId,String title,String message) {
        var publisher=(com.group49.support.util.TicketEventPublisher)getServletContext().getAttribute("ticketEvents");
        return publisher!=null && publisher.publish(new com.group49.support.model.TicketEvent(recipient,ticketId,title,message));
    }

    private void serveAttachment(HttpServletRequest req, HttpServletResponse res, User user) throws Exception {
        int attachmentId = WebUtil.parseInt(req.getParameter("id"), 0);
        TicketAttachment attachment = tickets.findAttachment(attachmentId, user).orElse(null);
        if (attachment == null) {
            res.sendError(404);
            return;
        }
        Path directory = uploadRoot().resolve(String.valueOf(attachment.ticketId())).normalize();
        Path file = directory.resolve(attachment.storedName()).normalize();
        if (!file.startsWith(directory)) { res.sendError(404); return; }
        if (!Files.isRegularFile(file)) file=uploadRoot().resolve(attachment.storedName()).normalize();
        if (!file.startsWith(uploadRoot()) || !Files.isRegularFile(file) || Files.isSymbolicLink(file)) {
            res.sendError(404, "Attachment file is missing");
            return;
        }
        res.setContentType(attachment.contentType());
        res.setContentLengthLong(Files.size(file));
        res.setHeader("Cache-Control", "private, max-age=300");
        String safeName = attachment.originalName().replace("\"", "").replace("\r", "").replace("\n", "");
        res.setHeader("Content-Disposition", (attachment.contentType().startsWith("image/") && !"1".equals(req.getParameter("download")) ? "inline" : "attachment") + "; filename=\"" + safeName.replaceAll("[^a-zA-Z0-9 ._()-]", "_") + "\"");
        Files.copy(file, res.getOutputStream());
    }

    private Path uploadRoot() {
        String configured = System.getProperty("helpify.upload.dir",System.getenv("HELPIFY_UPLOAD_DIR"));
        Path base = configured == null || configured.isBlank()
                ? Paths.get(System.getProperty("user.home"), "HelpifyUploads")
                : Paths.get(configured);
        return base.resolve("tickets").toAbsolutePath().normalize();
    }

    private String safeOriginalName(String submittedName) {
        if (submittedName == null || submittedName.isBlank()) return "screenshot.png";
        String normal = submittedName.replace('\\', '/');
        String name = normal.substring(normal.lastIndexOf('/') + 1).trim();
        name = name.replaceAll("[\\r\\n\\t]", "_");
        if (name.length() > 220) name = name.substring(name.length() - 220);
        return name.isBlank() ? "screenshot.png" : name;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }

    private void addMessage(HttpServletRequest req, Ticket ticket, User user) throws Exception {
        String message = WebUtil.value(req.getParameter("message"));
        if (message.length() < 2 || message.length() > 4000) {
            WebUtil.flash(req, "error", "Message must contain 2 to 4000 characters.");
            return;
        }
        boolean internal = !user.isCustomer() && "on".equals(req.getParameter("internal"));
        messages.create(ticket.id(), user.id(), message, internal);
        if (!internal) {
            int recipient = user.isCustomer() ? (ticket.assignedTo() == null ? 0 : ticket.assignedTo()) : ticket.customerId();
            if (recipient > 0) {
                if(!publish(recipient,ticket.id(),"New ticket message","A new message was added to "+ticket.number())) {
                    WebUtil.flash(req,"warning","Message saved, but notification could not be delivered."); return;
                }
            }
        }
        WebUtil.flash(req, "success", internal ? "Internal note added." : "Message sent.");
    }

    private void manageTicket(HttpServletRequest req, Ticket ticket, User user) throws Exception {
        String status = WebUtil.value(req.getParameter("status"));
        String priority = WebUtil.value(req.getParameter("priority"));
        if (!STATUSES.contains(status) || !PRIORITIES.contains(priority)) {
            WebUtil.flash(req, "error", "Invalid ticket status or priority.");
            return;
        }
        Integer assigned = WebUtil.parseInt(req.getParameter("assignedTo"), 0);
        if (assigned == 0) assigned = null;
        if (assigned != null && users.supportStaff().stream().noneMatch(staff -> staff.id() == WebUtil.parseInt(req.getParameter("assignedTo"),0))) {
            WebUtil.flash(req,"error","Choose an active support staff member."); return;
        }
        if(WebUtil.value(req.getParameter("note")).length()>500) { WebUtil.flash(req,"error","Note must be at most 500 characters."); return; }
        Integer previousAssignee = ticket.assignedTo();
        tickets.update(ticket.id(), status, priority, assigned, WebUtil.value(req.getParameter("note")), user.id());

        boolean notified = publish(ticket.customerId(),ticket.id(),"Ticket updated",ticket.number()+" is now "+status.replace('_',' '));

        if (assigned != null && !Objects.equals(previousAssignee, assigned)) {
            notified = publish(assigned,ticket.id(),"New ticket assignment",ticket.number()+" - "+ticket.subject()+" has been assigned to you.") && notified;
        }
        WebUtil.flash(req, notified?"success":"warning", notified?"Ticket updated. Relevant users were notified.":"Ticket updated, but a notification could not be delivered.");
    }

    private void view(HttpServletRequest req, HttpServletResponse res, String page)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/" + page).forward(req, res);
    }
}
