package com.library.booksystem.controller;

import com.library.booksystem.entity.Book;
import com.library.booksystem.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class PageController {

    @Autowired
    private BookRepository bookRepository;

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        List<Object[]> genreCounts = bookRepository.findAll().stream()
                .collect(Collectors.groupingBy(Book::getGenre, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
                .sorted((a,b) -> ((Long)b[1]).compareTo((Long)a[1]))
                .collect(Collectors.toList());

        model.addAttribute("genres", genreCounts);
        return "categories";
    }
}