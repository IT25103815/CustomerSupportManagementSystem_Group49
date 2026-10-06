<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Helpify | Customer Support</title>
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/assets/images/helpify-mark.svg">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css">
</head>
<body class="landing landing-home">
<%@ include file="WEB-INF/views/includes/icons.jsp" %>
<nav class="landing-nav">
    <a class="brand logo-brand" href="${pageContext.request.contextPath}/" aria-label="Helpify home"><img class="brand-logo brand-logo-public" src="${pageContext.request.contextPath}/assets/images/helpify-wordmark.svg" alt="Helpify"></a>
    <div><a class="nav-link icon-text" href="${pageContext.request.contextPath}/faqs"><svg class="ui-icon sm"><use href="#icon-faq"/></svg>Help Center</a><a class="btn btn-light" href="${pageContext.request.contextPath}/login">Sign In</a><a class="btn btn-primary" href="${pageContext.request.contextPath}/register">Get Started</a></div>
</nav>
<main class="hero simple-hero home-hero">
    <section class="hero-copy home-hero-copy">
        <span class="eyebrow">Customer support, organized</span>
        <h1>Manage support requests with <span class="home-title-accent">less clutter.</span></h1>
        <p>Create tickets, follow updates, message the support team and find answers from one clear workspace.</p>
        <div class="hero-actions"><a class="btn btn-primary btn-lg icon-text" href="${pageContext.request.contextPath}/tickets?action=new"><svg class="ui-icon sm"><use href="#icon-plus"/></svg>Create a ticket</a><a class="btn btn-light btn-lg icon-text" href="${pageContext.request.contextPath}/faqs"><svg class="ui-icon sm"><use href="#icon-search"/></svg>Browse Help Center</a></div>
    </section>
    <section class="hero-card support-overview home-support-card">
        <h2>Core support tools</h2>
        <ul>
            <li><svg class="ui-icon feature-svg"><use href="#icon-ticket"/></svg><div><b>Tickets</b><span>Create, assign and track requests.</span></div></li>
            <li><svg class="ui-icon feature-svg"><use href="#icon-faq"/></svg><div><b>Help Center</b><span>Find answers to common questions.</span></div></li>
            <li><svg class="ui-icon feature-svg"><use href="#icon-message"/></svg><div><b>Communication</b><span>Keep ticket conversations together.</span></div></li>
            <li><svg class="ui-icon feature-svg"><use href="#icon-reports"/></svg><div><b>Reports</b><span>Review real support activity.</span></div></li>
        </ul>
    </section>
</main>
<section class="feature-strip light-strip">
    <article><span class="feature-icon"><svg class="ui-icon"><use href="#icon-ticket"/></svg></span><h3>Ticket Workflow</h3><p>Status, priority and assignment at a glance.</p></article>
    <article><span class="feature-icon"><svg class="ui-icon"><use href="#icon-faq"/></svg></span><h3>Self-Service</h3><p>Published answers reduce repeated requests.</p></article>
    <article><span class="feature-icon"><svg class="ui-icon"><use href="#icon-reports"/></svg></span><h3>Real Data</h3><p>Support activity stays connected to SQL Server.</p></article>
</section>
<footer class="landing-footer">Helpify &middot; Customer Support Management</footer>
<script src="${pageContext.request.contextPath}/assets/js/app.js"></script></body></html>