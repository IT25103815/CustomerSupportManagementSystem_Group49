<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title><c:out value="${pageTitle}"/> | Helpify</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css">
</head>
<body class="app-body">
<aside class="sidebar" id="sidebar">
    <a class="brand app-brand" href="${pageContext.request.contextPath}/dashboard">
        <span class="brand-mark">H</span><span>Helpify<small>Customer Support</small></span>
    </a>
    <div class="role-chip">
        <span><c:out value="${currentUser.fullName.substring(0,1)}"/></span>
        <div><b><c:out value="${currentUser.fullName}"/></b><small><c:out value="${currentUser.roleLabel}"/></small></div>
    </div>
    <nav class="side-nav">
        <a class="${pageKey eq 'dashboard'?'active':''}" href="${pageContext.request.contextPath}/dashboard"><i>DB</i> Dashboard</a>
        <a class="${pageKey eq 'tickets'?'active':''}" href="${pageContext.request.contextPath}/tickets"><i>TK</i> ${currentUser.customer?'My Tickets':'Ticket Queue'}</a>
        <a class="${pageKey eq 'faqs'?'active':''}" href="${pageContext.request.contextPath}/faqs"><i>KB</i> Knowledge Base</a>
        <a class="${pageKey eq 'feedback'?'active':''}" href="${pageContext.request.contextPath}/feedback"><i>FB</i> Feedback</a>
        <a class="${pageKey eq 'notifications'?'active':''}" href="${pageContext.request.contextPath}/notifications"><i>NT</i> Notifications<c:if test="${unreadCount gt 0}"><em>${unreadCount}</em></c:if></a>
        <c:if test="${currentUser.role eq 'IT_SUPPORT_COORDINATOR' or currentUser.role eq 'CUSTOMER_SUPPORT_MANAGER'}">
            <a class="${pageKey eq 'users'?'active':''}" href="${pageContext.request.contextPath}/users"><i>UA</i> User Access</a>
        </c:if>
        <c:if test="${currentUser.role eq 'CUSTOMER_SUPPORT_MANAGER' or currentUser.role eq 'OPERATIONS_EXECUTIVE' or currentUser.role eq 'QUALITY_ASSURANCE_SUPERVISOR'}">
            <a class="${pageKey eq 'reports'?'active':''}" href="${pageContext.request.contextPath}/reports"><i>RP</i> Analytics & Reports</a>
        </c:if>
    </nav>
    <div class="sidebar-foot">
        <a href="${pageContext.request.contextPath}/profile">Profile & Security</a>
        <form method="post" action="${pageContext.request.contextPath}/logout">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <button type="submit">Sign Out</button>
        </form>
    </div>
</aside>
<div class="app-shell">
    <header class="topbar">
        <button class="menu-btn" data-menu type="button">Menu</button>
        <div><span class="top-context">Helpify Support Workspace</span><h1><c:out value="${pageTitle}"/></h1></div>
        <div class="top-actions">
            <a class="notification-button" href="${pageContext.request.contextPath}/notifications">Alerts<c:if test="${unreadCount gt 0}"><b>${unreadCount}</b></c:if></a>
            <a class="avatar" href="${pageContext.request.contextPath}/profile"><c:out value="${currentUser.fullName.substring(0,1)}"/></a>
        </div>
    </header>
    <main class="content">
        <c:if test="${not empty sessionScope.flashMessage}">
            <div class="alert alert-${sessionScope.flashType} toast"><c:out value="${sessionScope.flashMessage}"/></div>
            <c:remove var="flashMessage" scope="session"/><c:remove var="flashType" scope="session"/>
        </c:if>
