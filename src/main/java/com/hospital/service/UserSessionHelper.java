package com.hospital.service;

import com.hospital.model.Role;
import com.hospital.model.User;
import jakarta.servlet.http.HttpSession;

public class UserSessionHelper {

    public static User getLoggedInPatient(HttpSession session) {
        if (session == null) return null;
        User user = (User) session.getAttribute("loggedInPatient");
        if (user != null && user.getRole() == Role.PATIENT) {
            return user;
        }
        User global = (User) session.getAttribute("loggedInUser");
        if (global != null && global.getRole() == Role.PATIENT) {
            return global;
        }
        return null;
    }

    public static User getLoggedInDoctor(HttpSession session) {
        if (session == null) return null;
        User user = (User) session.getAttribute("loggedInDoctor");
        if (user != null && user.getRole() == Role.DOCTOR) {
            return user;
        }
        User global = (User) session.getAttribute("loggedInUser");
        if (global != null && global.getRole() == Role.DOCTOR) {
            return global;
        }
        return null;
    }

    public static User getLoggedInAdmin(HttpSession session) {
        if (session == null) return null;
        User user = (User) session.getAttribute("loggedInAdmin");
        if (user != null && user.getRole() == Role.ADMIN) {
            return user;
        }
        User global = (User) session.getAttribute("loggedInUser");
        if (global != null && global.getRole() == Role.ADMIN) {
            return global;
        }
        return null;
    }

    public static User getLoggedInVendor(HttpSession session) {
        if (session == null) return null;
        User user = (User) session.getAttribute("loggedInVendor");
        if (user != null && user.getRole() == Role.VENDOR) {
            return user;
        }
        User global = (User) session.getAttribute("loggedInUser");
        if (global != null && global.getRole() == Role.VENDOR) {
            return global;
        }
        return null;
    }

    public static User getLoggedInUserForRole(HttpSession session, Role role) {
        if (role == Role.PATIENT) return getLoggedInPatient(session);
        if (role == Role.DOCTOR) return getLoggedInDoctor(session);
        if (role == Role.ADMIN) return getLoggedInAdmin(session);
        if (role == Role.VENDOR) return getLoggedInVendor(session);
        return (User) session.getAttribute("loggedInUser");
    }

    public static User getAnyLoggedInUser(HttpSession session) {
        if (session == null) return null;
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null) return user;

        user = (User) session.getAttribute("loggedInPatient");
        if (user != null) return user;

        user = (User) session.getAttribute("loggedInDoctor");
        if (user != null) return user;

        user = (User) session.getAttribute("loggedInAdmin");
        if (user != null) return user;

        return (User) session.getAttribute("loggedInVendor");
    }

    public static void setLoggedInUserForRole(HttpSession session, User user) {
        if (session == null || user == null) return;
        session.setAttribute("loggedInUser", user);
        switch (user.getRole()) {
            case PATIENT -> session.setAttribute("loggedInPatient", user);
            case DOCTOR -> session.setAttribute("loggedInDoctor", user);
            case ADMIN -> session.setAttribute("loggedInAdmin", user);
            case VENDOR -> session.setAttribute("loggedInVendor", user);
        }
    }

    public static boolean removeLoggedInUserForRole(HttpSession session, String roleStr) {
        if (session == null || roleStr == null) return false;
        String r = roleStr.trim().toLowerCase();
        switch (r) {
            case "patient" -> session.removeAttribute("loggedInPatient");
            case "doctor" -> session.removeAttribute("loggedInDoctor");
            case "admin" -> session.removeAttribute("loggedInAdmin");
            case "vendor", "pharmacy", "laboratory" -> session.removeAttribute("loggedInVendor");
            default -> { return false; }
        }

        // Check if any other role remains in session
        if (session.getAttribute("loggedInPatient") == null &&
            session.getAttribute("loggedInDoctor") == null &&
            session.getAttribute("loggedInAdmin") == null &&
            session.getAttribute("loggedInVendor") == null) {
            session.removeAttribute("loggedInUser");
            session.invalidate();
            return true;
        } else {
            // Update fallback loggedInUser to remaining active role
            User nextActive = getAnyLoggedInUser(session);
            if (nextActive != null) {
                session.setAttribute("loggedInUser", nextActive);
            }
        }
        return false;
    }
}
