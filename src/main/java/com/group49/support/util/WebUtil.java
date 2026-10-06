package com.group49.support.util;

import com.group49.support.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.regex.Pattern;

public final class WebUtil {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9\\s-]{6,19}$");

    private WebUtil() {}

    public static User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("currentUser");
    }

    public static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); }
        catch (Exception exception) { return fallback; }
    }

    public static String value(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean validEmail(String value) {
        return value != null && EMAIL.matcher(value.trim()).matches();
    }

    public static boolean validPhone(String value) {
        String phone = value(value);
        return phone.isEmpty() || PHONE.matcher(phone).matches();
    }

    public static void flash(HttpServletRequest request, String type, String message) {
        request.getSession().setAttribute("flashType", type);
        request.getSession().setAttribute("flashMessage", message);
    }
}
