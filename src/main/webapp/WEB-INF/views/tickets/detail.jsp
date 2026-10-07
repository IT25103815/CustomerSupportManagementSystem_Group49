<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Ticket ${ticket.number}"/>
<c:set var="pageKey" value="tickets"/>
<%@ include file="../includes/head.jsp" %>

<div class="ticket-header">
    <div>
        <a class="back-link icon-text" href="${pageContext.request.contextPath}/tickets">
            <svg class="ui-icon sm"><use href="#icon-back"/></svg>Tickets
        </a>
        <span class="eyebrow"><c:out value="${ticket.categoryName}"/></span>
        <h2><c:out value="${ticket.subject}"/></h2>
        <p>Created by <b><c:out value="${ticket.customerName}"/></b> &middot; ${ticket.createdAt}</p>
    </div>
    <div class="ticket-badges">
        <span class="badge priority-${ticket.priority.toLowerCase()}">${ticket.priority}</span>
        <span class="badge status-${ticket.status.toLowerCase().replace('_','-')}">${ticket.status.replace('_',' ')}</span>
    </div>
</div>

<c:if test="${customerCanEdit}">
    <div class="notice-bar">
        <span>Open and unassigned — editing is available.</span>
        <div>
            <a class="btn btn-light icon-text" href="${pageContext.request.contextPath}/tickets?action=edit&id=${ticket.id}">
                <svg class="ui-icon sm"><use href="#icon-edit"/></svg>Edit
            </a>
            <form method="post" action="${pageContext.request.contextPath}/tickets" data-confirm="Cancel this ticket?" class="inline-form">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="cancel">
                <input type="hidden" name="id" value="<c:out value='${ticket.id}'/>">
                <button class="btn btn-danger">Cancel</button>
            </form>
        </div>
    </div>
</c:if>

<div class="ticket-detail-grid">
    <section class="panel conversation">
        <div class="original-message">
            <span>Original request</span>
            <p><c:out value="${ticket.description}"/></p>
        </div>

        <section class="ticket-attachments">
            <div class="attachment-heading">
                <div>
                    <span class="section-kicker">Evidence</span>
                    <h3 class="icon-text"><svg class="ui-icon sm"><use href="#icon-attachment"/></svg>Attachments</h3>
                </div>
                <span class="badge"><c:out value="${attachments.size()}"/></span>
            </div>

            <c:choose>
                <c:when test="${empty attachments}">
                    <div class="attachment-empty">
                        <svg class="ui-icon"><use href="#icon-image"/></svg>
                        <span>No attachments yet.</span>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="attachment-grid">
                        <c:forEach items="${attachments}" var="attachment">
                            <a class="attachment-card" target="_blank" rel="noopener"
                               href="${pageContext.request.contextPath}/tickets?action=attachment&id=${attachment.id}"
                               title="Open or download attachment">
                                <c:choose><c:when test="${attachment.contentType eq 'image/png' or attachment.contentType eq 'image/jpeg'}"><img loading="lazy" src="${pageContext.request.contextPath}/tickets?action=attachment&amp;id=${attachment.id}" alt="Attached image"></c:when><c:otherwise><span class="document-mark">FILE</span></c:otherwise></c:choose>
                                <span class="attachment-info">
                                    <b><c:out value="${attachment.originalName}"/></b>
                                    <small><c:out value="${attachment.fileSizeKb}"/> KB &middot; <c:out value="${attachment.uploaderName}"/></small>
                                </span>
                                <svg class="ui-icon sm"><use href="#icon-eye"/></svg>
                            </a>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>

            <c:if test="${customerCanUpload}">
                <form method="post" enctype="multipart/form-data"
                      action="${pageContext.request.contextPath}/tickets?action=uploadAttachment&id=${ticket.id}"
                      class="attachment-upload">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <label class="attachment-picker">
                        <span class="icon-text"><svg class="ui-icon sm"><use href="#icon-image"/></svg>Add supporting file</span>
                        <input type="file" name="attachment" accept=".png,.jpg,.jpeg,.pdf,.doc,.docx" data-attachments data-max-files="1" data-max-bytes="10485760" required>
                        <small>PNG/JPG, PDF, DOC/DOCX &middot; Maximum 10 MB</small><span class="selected-files" aria-live="polite"></span>
                    </label>
                    <button class="btn btn-light icon-text" type="submit">
                        <svg class="ui-icon sm"><use href="#icon-upload"/></svg>Upload
                    </button>
                </form>
            </c:if>
        </section>

        <div class="message-list">
            <c:forEach items="${messages}" var="message">
                <article class="message ${message.senderId eq currentUser.id?'mine':''} ${message.internalNote?'internal':''}">
                    <div class="message-avatar"><c:out value="${message.senderName.substring(0,1)}"/></div>
                    <div>
                        <header>
                            <b><c:out value="${message.senderName}"/></b>
                            <span>${message.sentAt}</span>
                            <c:if test="${message.internalNote}"><em>Internal note</em></c:if>
                        </header>
                        <p><c:out value="${message.message}"/></p>
                    </div>
                </article>
            </c:forEach>
        </div>

        <c:choose>
            <c:when test="${canMessage}">
                <form method="post" action="${pageContext.request.contextPath}/tickets" class="reply-box">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="message">
                    <input type="hidden" name="id" value="<c:out value='${ticket.id}'/>">
                    <textarea name="message" rows="3" minlength="2" maxlength="4000" required placeholder="Write a reply..."></textarea>
                    <div>
                        <c:if test="${not currentUser.customer}">
                            <label class="check"><input type="checkbox" name="internal"> Internal note</label>
                        </c:if>
                        <button class="btn btn-primary icon-text"><svg class="ui-icon sm"><use href="#icon-message"/></svg>Send</button>
                    </div>
                </form>
            </c:when>
            <c:otherwise>
                <div class="read-only-note">Your role has view-only access to ticket messages.</div>
            </c:otherwise>
        </c:choose>
    </section>

    <aside class="ticket-side">
        <c:if test="${currentUser.role eq 'SENIOR_CUSTOMER_SERVICE_OFFICER' or currentUser.role eq 'OPERATIONS_EXECUTIVE' or currentUser.role eq 'CUSTOMER_SUPPORT_MANAGER'}">
            <section class="panel">
                <div class="panel-head"><h3>Manage Ticket</h3></div>
                <form method="post" action="${pageContext.request.contextPath}/tickets" class="form-stack compact">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="update">
                    <input type="hidden" name="id" value="<c:out value='${ticket.id}'/>">
                    <label>Status
                        <select name="status">
                            <c:forEach items="${['OPEN','ASSIGNED','IN_PROGRESS','WAITING_FOR_CUSTOMER','ESCALATED','RESOLVED','CLOSED','CANCELLED','REOPENED']}" var="s">
                                <option value="<c:out value='${s}'/>" ${ticket.status eq s?'selected':''}>${s.replace('_',' ')}</option>
                            </c:forEach>
                        </select>
                    </label>
                    <label>Priority
                        <select name="priority">
                            <c:forEach items="${['LOW','MEDIUM','HIGH','URGENT']}" var="p">
                                <option value="<c:out value='${p}'/>" ${ticket.priority eq p?'selected':''}>${p}</option>
                            </c:forEach>
                        </select>
                    </label>
                    <label>Assign to
                        <select name="assignedTo">
                            <option value="">Unassigned</option>
                            <c:forEach items="${staff}" var="person">
                                <option value="<c:out value='${person.id}'/>" ${ticket.assignedTo eq person.id?'selected':''}><c:out value="${person.fullName}"/></option>
                            </c:forEach>
                        </select>
                    </label>
                    <label>Update note<textarea name="note" rows="2" maxlength="500" placeholder="Reason for this change"></textarea></label>
                    <button class="btn btn-secondary btn-block icon-text"><svg class="ui-icon sm"><use href="#icon-save"/></svg>Save</button>
                </form>
            </section>
        </c:if>

        <c:if test="${currentUser.role eq 'CUSTOMER_SUPPORT_MANAGER'}">
            <section class="panel danger-panel">
                <div class="panel-head"><div><h3>Delete Ticket</h3><p>Permanently removes this ticket and its related ticket data.</p></div></div>
                <form method="post" action="${pageContext.request.contextPath}/tickets" class="form-stack compact" data-confirm="Permanently delete this ticket and all related ticket data? This action cannot be undone.">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="action" value="delete">
                    <input type="hidden" name="id" value="<c:out value='${ticket.id}'/>">
                    <button class="btn btn-danger btn-block icon-text"><svg class="ui-icon sm"><use href="#icon-trash"/></svg>Delete Ticket</button>
                </form>
            </section>
        </c:if>

        <section class="panel timeline-panel">
            <div class="panel-head"><h3>Activity History</h3></div>
            <div class="timeline">
                <c:forEach items="${history}" var="item">
                    <article><i></i><div><b>${item.newStatus.replace('_',' ')}</b><span><c:out value="${item.name}"/> &middot; ${item.date}</span><small><c:out value="${item.note}"/></small></div></article>
                </c:forEach>
            </div>
        </section>
    </aside>
</div>
<%@ include file="../includes/foot.jsp" %>
