package com.library.booksystem.controller;

import com.library.booksystem.entity.Book;
import com.library.booksystem.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminToolsController {

    @Autowired
    private BookRepository bookRepository;

    // Временно – для принудительного обновления описаний у всех книг
    @GetMapping("/fill-descriptions")
    @ResponseBody
    public String forceFillDescriptions() {
        List<Book> allBooks = bookRepository.findAll();
        int updated = 0;

        for (Book book : allBooks) {
            boolean changed = false;

            // Стандартное описание
            if (book.getDescription() == null || book.getDescription().trim().isEmpty() || book.getDescription().contains("— это увлекательное произведение")) {
                book.setDescription("📖 " + book.getTitle() + " — классическое произведение, которое стоит прочитать. " +
                        "Книга раскрывает глубокие темы и оставляет яркое впечатление. Рекомендуется всем любителям литературы.");
                changed = true;
            }

            // Биография автора
            if (book.getAuthorBio() == null || book.getAuthorBio().trim().isEmpty() || book.getAuthorBio().contains("— талантливый автор")) {
                book.setAuthorBio("✍️ " + book.getAuthor() + " — известный писатель, чьи произведения входят в золотой фонд мировой литературы.");
                changed = true;
            }

            // Издательство
            if (book.getPublisher() == null || book.getPublisher().trim().isEmpty()) {
                book.setPublisher("Издательство \"Колледж-Пресс\"");
                changed = true;
            }

            // ISBN
            if (book.getIsbn() == null || book.getIsbn().trim().isEmpty()) {
                book.setIsbn("978-5-00000-" + String.format("%04d", book.getId()));
                changed = true;
            }

            // Страницы
            if (book.getPages() == null || book.getPages() <= 0) {
                book.setPages(300);
                changed = true;
            }

            if (changed) {
                bookRepository.save(book);
                updated++;
            }
        }

        return "Обновлено книг: " + updated + " из " + allBooks.size();
    }
}