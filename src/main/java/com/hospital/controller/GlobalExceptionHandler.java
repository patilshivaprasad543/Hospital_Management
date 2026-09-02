package com.hospital.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, HttpServletRequest request, Model model) {
        logger.error("Unhandled HTTP 500 error at URI [{}]: ", request.getRequestURI(), ex);

        String userMessage = ex.getMessage();
        if (userMessage == null || userMessage.contains("SQL") || userMessage.contains("Constraint") || userMessage.contains("JDBC")) {
            userMessage = "A system database error occurred. Please verify input data and try again.";
        }

        model.addAttribute("status", 500);
        model.addAttribute("errorMessage", userMessage);
        return "error";
    }
}
