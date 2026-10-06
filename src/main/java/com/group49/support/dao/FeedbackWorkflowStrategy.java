package com.group49.support.dao;
import com.group49.support.model.User;
import com.group49.support.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
public interface FeedbackWorkflowStrategy { void execute(HttpServletRequest req,User user,String action) throws Exception; }
