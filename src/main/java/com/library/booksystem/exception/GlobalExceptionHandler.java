package com.library.booksystem.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllExceptions(Exception ex) {
        ex.printStackTrace(); // печать ошибки в консоль сервера
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorMessage", "Ошибка: " + ex.getMessage());
        return mav;
    }
}