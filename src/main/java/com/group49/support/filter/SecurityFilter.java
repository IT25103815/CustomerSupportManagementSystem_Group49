package com.group49.support.filter;

import com.group49.support.model.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;

@WebFilter("/*")
public class SecurityFilter implements Filter {
    @Override public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain)throws IOException,ServletException{
        HttpServletRequest req=(HttpServletRequest)request;HttpServletResponse res=(HttpServletResponse)response;
        req.setCharacterEncoding("UTF-8");res.setCharacterEncoding("UTF-8");
        res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("X-Frame-Options","SAMEORIGIN");res.setHeader("Referrer-Policy","strict-origin-when-cross-origin");
        HttpSession session=req.getSession();if(session.getAttribute("csrfToken")==null)session.setAttribute("csrfToken",UUID.randomUUID().toString());
        String path=req.getRequestURI().substring(req.getContextPath().length());
        boolean staticPath=path.startsWith("/assets/");
        boolean publicPath=path.equals("/")||path.equals("/index.jsp")||path.equals("/login")||path.equals("/register")||path.equals("/forgot-password")||path.equals("/reset-password")||(path.equals("/faqs")&&"GET".equals(req.getMethod()));
        if("POST".equalsIgnoreCase(req.getMethod())&&!staticPath){String expected=(String)session.getAttribute("csrfToken");String supplied=req.getParameter("csrfToken");if(expected==null||!expected.equals(supplied)){res.sendError(403,"Invalid security token");return;}}
        User user=(User)session.getAttribute("currentUser");
        if(!staticPath&&!publicPath&&user==null){session.setAttribute("flashType","warning");session.setAttribute("flashMessage","Please sign in to continue.");res.sendRedirect(req.getContextPath()+"/login");return;}
        req.setAttribute("currentUser",user);chain.doFilter(request,response);
    }
}
