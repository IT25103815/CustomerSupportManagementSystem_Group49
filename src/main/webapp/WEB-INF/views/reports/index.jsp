<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Helpify | Customer Support</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css">
</head>
<body class="landing">
<nav class="landing-nav">
    <a class="brand" href="${pageContext.request.contextPath}/"><span class="brand-mark">H</span><span>Helpify<small>Customer Support</small></span></a>
    <div><a class="nav-link" href="${pageContext.request.contextPath}/faqs">Knowledge Base</a><a class="btn btn-light" href="${pageContext.request.contextPath}/login">Sign In</a><a class="btn btn-primary" href="${pageContext.request.contextPath}/register">Create Account</a></div>
</nav>
<main class="hero simple-hero">
    <section class="hero-copy">
        <span class="eyebrow">Reliable customer support</span>
        <h1>Support requests, clearly managed from start to finish.</h1>
        <p>Helpify gives customers and support teams one place to submit requests, track progress, communicate, find answers and review service quality.</p>
        <div class="hero-actions"><a class="btn btn-primary btn-lg" href="${pageContext.request.contextPath}/register">Create Customer Account</a><a class="btn btn-light btn-lg" href="${pageContext.request.contextPath}/faqs">Browse FAQs</a></div>
    </section>
    <section class="hero-card">
        <h2>What Helpify supports</h2>
        <ul><li><b>Ticket Management</b><span>Create, assign, update and track support requests.</span></li><li><b>Knowledge Base</b><span>Search practical answers to common questions.</span></li><li><b>Communication</b><span>Keep ticket messages and notifications organized.</span></li><li><b>Service Insights</b><span>Use database-backed dashboards and reports.</span></li></ul>
    </section>
</main>
<section class="feature-strip light-strip"><article><span class="feature-icon">01</span><h3>Organized Tickets</h3><p>Clear status, priority, assignment and activity history.</p></article><article><span class="feature-icon">02</span><h3>Self-Service Help</h3><p>Published FAQs help customers resolve common issues quickly.</p></article><article><span class="feature-icon">03</span><h3>Real Data</h3><p>Support activity is stored and retrieved through Microsoft SQL Server.</p></article></section>
<footer class="landing-footer">Helpify &middot; Web-Based Customer Support Management System</footer>
</body></html>
