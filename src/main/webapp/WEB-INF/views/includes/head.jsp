<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title><c:out value="${pageTitle}"/> | Helpify</title>
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/assets/images/helpify-mark.svg">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css">
</head>
<body class="app-body">
<%@ include file="icons.jsp" %>
<aside class="sidebar" id="sidebar">
    <a class="brand app-brand logo-brand" href="${pageContext.request.contextPath}/dashboard" aria-label="Helpify dashboard">
        <img class="brand-icon" src="${pageContext.request.contextPath}/assets/images/helpify-mark.svg" alt="">
        <span>Helpify<small>Customer Support</small></span>
    </a>
    <div class="role-chip">
        <span><c:out value="${currentUser.fullName.substring(0,1)}"/></span>
        <div><b><c:out value="${currentUser.fullName}"/></b><small><c:out value="${currentUser.roleLabel}"/></small></div>
    </div>
    <nav class="side-nav">
        <a class="${pageKey eq 'dashboard'?'active':''}" href="${pageContext.request.contextPath}/dashboard"><i><svg class="ui-icon"><use href="#icon-dashboard"/></svg></i><span>Dashboard</span></a>
        <a class="${pageKey eq 'tickets'?'active':''}" href="${pageContext.request.contextPath}/tickets"><i><svg class="ui-icon"><use href="#icon-ticket"/></svg></i><span>${currentUser.customer?'My Tickets':'Ticket Queue'}</span></a>
        <a class="${pageKey eq 'faqs'?'active':''}" href="${pageContext.request.contextPath}/faqs"><i><svg class="ui-icon"><use href="#icon-faq"/></svg></i><span>Help Center</span></a>
        <a class="${pageKey eq 'feedback'?'active':''}" href="${pageContext.request.contextPath}/feedback"><i><svg class="ui-icon"><use href="#icon-feedback"/></svg></i><span>Feedback</span></a>
        <a class="${pageKey eq 'notifications'?'active':''}" href="${pageContext.request.contextPath}/notifications"><i><svg class="ui-icon"><use href="#icon-bell"/></svg></i><span>Notifications</span><c:if test="${unreadCount gt 0}"><em>${unreadCount}</em></c:if></a>
        <c:if test="${currentUser.role eq 'IT_SUPPORT_COORDINATOR' or currentUser.role eq 'CUSTOMER_SUPPORT_MANAGER'}">
            <a class="${pageKey eq 'users'?'active':''}" href="${pageContext.request.contextPath}/users"><i><svg class="ui-icon"><use href="#icon-users"/></svg></i><span>Users</span></a>
        </c:if>
        <c:if test="${currentUser.role eq 'CUSTOMER_SUPPORT_MANAGER' or currentUser.role eq 'OPERATIONS_EXECUTIVE' or currentUser.role eq 'QUALITY_ASSURANCE_SUPERVISOR'}">
            <a class="${pageKey eq 'reports'?'active':''}" href="${pageContext.request.contextPath}/reports"><i><svg class="ui-icon"><use href="#icon-reports"/></svg></i><span>Reports</span></a>
        </c:if>
    </nav>
    <div class="sidebar-foot">
        <a class="side-foot-link" href="${pageContext.request.contextPath}/profile"><svg class="ui-icon sm"><use href="#icon-profile"/></svg><span>Profile</span></a>
        <form method="post" action="${pageContext.request.contextPath}/logout">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <button class="side-foot-link" type="submit"><svg class="ui-icon sm"><use href="#icon-logout"/></svg><span>Sign Out</span></button>
        </form>
    </div>
</aside>
<div class="app-shell">
    <header class="topbar">
        <button class="menu-btn icon-btn" data-menu type="button" title="Open menu" aria-label="Open menu"><svg class="ui-icon"><use href="#icon-menu"/></svg></button>
        <div><span class="top-context">Customer Support</span><h1><c:out value="${pageTitle}"/></h1></div>
        <div class="top-actions">
            <a class="notification-button icon-btn" href="${pageContext.request.contextPath}/notifications" title="Notifications" aria-label="Notifications"><svg class="ui-icon"><use href="#icon-bell"/></svg><c:if test="${unreadCount gt 0}"><b>${unreadCount}</b></c:if></a>
            <a class="avatar" href="${pageContext.request.contextPath}/profile" title="Profile" aria-label="Profile"><c:out value="${currentUser.fullName.substring(0,1)}"/></a>
        </div>
    </header>
    <main class="content">
        <c:if test="${not empty sessionScope.flashMessage}">
            <div class="alert alert-${sessionScope.flashType} toast"><c:out value="${sessionScope.flashMessage}"/></div>
            <c:remove var="flashMessage" scope="session"/><c:remove var="flashType" scope="session"/>
        </c:if>
