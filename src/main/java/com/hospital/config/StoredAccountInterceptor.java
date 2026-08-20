package com.hospital.config;

import com.hospital.model.User;
import com.hospital.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class StoredAccountInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return true;
        }
        Object raw = session.getAttribute("loggedInUser");
        if (!(raw instanceof User sessionUser) || sessionUser.getId() == null) {
            return true;
        }
        userService.findById(sessionUser.getId()).ifPresentOrElse(
                stored -> session.setAttribute("loggedInUser", stored),
                () -> session.removeAttribute("loggedInUser"));
        return true;
    }
}
