package com.library.booksystem.controller;

import com.library.booksystem.entity.Book;
import com.library.booksystem.repository.BookRepository;
import com.library.booksystem.repository.BookRequestRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class BookController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookRequestRepository bookRequestRepository;

    @Value("${book.cover.upload-dir:src/main/resources/static/images/books/}")
    private String uploadDir;

    // Передаёт версию (текущее время) во все шаблоны для обновления кэша обложек
    @ModelAttribute("coverVersion")
    public Long getCoverVersion() {
        return System.currentTimeMillis();
    }

    @GetMapping("/")
    public String showHomePage(Model model,
                               @RequestParam(required = false) String search,
                               @RequestParam(required = false) String trendingGenre) {
        try {
            List<Book> recentlyAdded = bookRepository.findTop6ByOrderByCreatedAtDesc();
            model.addAttribute("recentlyAdded", recentlyAdded != null ? recentlyAdded : List.of());

            List<Object[]> genreCounts = bookRepository.findAll().stream()
                    .filter(book -> book != null && book.getGenre() != null)
                    .collect(Collectors.groupingBy(Book::getGenre, Collectors.counting()))
                    .entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
                    .collect(Collectors.toList());
            model.addAttribute("popularCategories", genreCounts);

            List<Book> trendingBooks;
            if (trendingGenre != null && !trendingGenre.isEmpty()) {
                trendingBooks = bookRepository.findTop6ByGenreOrderByCreatedAtDesc(trendingGenre);
            } else {
                List<Book> allBooks = bookRepository.findAll();
                trendingBooks = allBooks.stream()
                        .filter(b -> b != null && b.getId() != null)
                        .sorted((b1, b2) -> Long.compare(
                                bookRequestRepository.countByBookId(b2.getId()),
                                bookRequestRepository.countByBookId(b1.getId())))
                        .limit(6)
                        .collect(Collectors.toList());
            }
            model.addAttribute("trendingBooks", trendingBooks != null ? trendingBooks : List.of());
            model.addAttribute("selectedTrendingGenre", trendingGenre);

            List<Book> searchResults = null;
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase().trim();
                searchResults = bookRepository.findAll().stream()
                        .filter(book -> book != null &&
                                ((book.getTitle() != null && book.getTitle().toLowerCase().contains(searchLower)) ||
                                        (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(searchLower)) ||
                                        (book.getGenre() != null && book.getGenre().toLowerCase().contains(searchLower))))
                        .collect(Collectors.toList());
                model.addAttribute("searchResults", searchResults);
                model.addAttribute("searchQuery", search);
            }

            List<String> allGenres = bookRepository.findAll().stream()
                    .map(Book::getGenre)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            model.addAttribute("allGenres", allGenres);

            return "index";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Ошибка при загрузке страницы: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/search")
    public String searchBooks(@RequestParam(required = false) String query) {
        if (query == null || query.trim().isEmpty()) {
            return "redirect:/";
        }
        String encodedQuery = java.net.URLEncoder.encode(query.trim(), java.nio.charset.StandardCharsets.UTF_8);
        return "redirect:/?search=" + encodedQuery;
    }

    @GetMapping("/add")
    @PreAuthorize("hasRole('LIBRARIAN') or hasRole('ADMIN')")
    public String showAddBookForm(Model model) {
        model.addAttribute("book", new Book());
        return "add-book";
    }

    @GetMapping("/category/{genre}")
    public String showCategoryBooks(@PathVariable String genre, Model model) {
        List<Book> books = bookRepository.findByGenre(genre);
        model.addAttribute("books", books);
        model.addAttribute("genre", genre);
        return "category-books";
    }

    @GetMapping("/books/all")
    public String allBooks(Model model) {
        List<Book> allBooks = bookRepository.findAll();
        model.addAttribute("books", allBooks);
        model.addAttribute("title", "Все книги");
        return "books-list";
    }

    @GetMapping("/books/recent")
    public String recentBooks(Model model) {
        List<Book> recent = bookRepository.findTop10ByOrderByCreatedAtDesc();
        model.addAttribute("books", recent);
        model.addAttribute("title", "Недавно добавленные");
        return "books-list";
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('LIBRARIAN') or hasRole('ADMIN')")
    public String saveBook(@Valid @ModelAttribute Book book, BindingResult result,
                           @RequestParam(value = "coverFile", required = false) MultipartFile coverFile) {
        if (result.hasErrors()) {
            return "add-book";
        }

        bookRepository.save(book);

        if (coverFile != null && !coverFile.isEmpty()) {
            saveCoverImage(book.getId(), coverFile);
            book.setCoverUrl("/images/books/" + book.getId() + ".jpg");
        } else if (book.getCoverUrl() == null || book.getCoverUrl().isEmpty()) {
            book.setCoverUrl("/images/default-cover.jpg");
        }
        bookRepository.save(book);

        return "redirect:/";
    }

    @PostMapping("/update/{id}")
    @PreAuthorize("hasRole('LIBRARIAN') or hasRole('ADMIN')")
    public String updateBook(@PathVariable Long id, @Valid @ModelAttribute Book book, BindingResult result,
                             @RequestParam(value = "coverFile", required = false) MultipartFile coverFile) {
        if (result.hasErrors()) {
            return "edit-book";
        }

        Book existingBook = bookRepository.findById(id).orElseThrow();
        existingBook.setTitle(book.getTitle());
        existingBook.setAuthor(book.getAuthor());
        existingBook.setGenre(book.getGenre());
        existingBook.setPublicationYear(book.getPublicationYear());
        existingBook.setCopies(book.getCopies());
        existingBook.setDescription(book.getDescription());
        existingBook.setAuthorBio(book.getAuthorBio());
        existingBook.setPublisher(book.getPublisher());
        existingBook.setIsbn(book.getIsbn());
        existingBook.setPages(book.getPages());

        if (coverFile != null && !coverFile.isEmpty()) {
            saveCoverImage(id, coverFile);
            existingBook.setCoverUrl("/images/books/" + id + ".jpg");
        } else if (book.getCoverUrl() != null && !book.getCoverUrl().isEmpty()) {
            existingBook.setCoverUrl(book.getCoverUrl());
        }

        bookRepository.save(existingBook);
        return "redirect:/";
    }

    private void saveCoverImage(Long bookId, MultipartFile file) {
        if (file == null || file.isEmpty()) return;
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String fileName = bookId + ".jpg";
            Path filePath = uploadPath.resolve(fileName);
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image != null) {
                ImageIO.write(image, "jpg", filePath.toFile());
            } else {
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('LIBRARIAN') or hasRole('ADMIN')")
    public String showEditForm(@PathVariable Long id, Model model) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Неверный ID книги: " + id));
        List<String> genres = bookRepository.findAll().stream()
                .map(Book::getGenre)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        model.addAttribute("book", book);
        model.addAttribute("genres", genres);
        return "edit-book";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('LIBRARIAN') or hasRole('ADMIN')")
    public String deleteBook(@PathVariable Long id) {
        try {
            Path coverPath = Paths.get(uploadDir, id + ".jpg");
            Files.deleteIfExists(coverPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        bookRepository.deleteById(id);
        return "redirect:/";
    }

    @GetMapping("/book/{id}")
    public String showBookDetails(@PathVariable Long id, Model model) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Неверный ID книги: " + id));
        model.addAttribute("book", book);
        List<Book> similarBooks = findSimilarBooksAdvanced(book, 5);
        model.addAttribute("similarBooks", similarBooks);
        return "book-details";
    }

    @GetMapping("/stats")
    public String showStatistics(Model model) {
        List<Book> allBooks = bookRepository.findAll();
        long totalBooks = allBooks.size();
        int totalCopies = allBooks.stream().mapToInt(Book::getCopies).sum();
        Map<String, Long> booksByGenre = allBooks.stream()
                .filter(book -> book.getGenre() != null)
                .collect(Collectors.groupingBy(Book::getGenre, Collectors.counting()));
        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("totalCopies", totalCopies);
        model.addAttribute("booksByGenre", booksByGenre);
        return "stats";
    }

    @GetMapping("/database")
    @PreAuthorize("hasRole('LIBRARIAN') or hasRole('ADMIN')")
    public String showDatabaseInfo(Model model) {
        model.addAttribute("dbUrl", "jdbc:mysql://localhost:3306/librarydb");
        model.addAttribute("dbUser", "root");
        model.addAttribute("dbPassword", "********");
        return "database-info";
    }

    private List<Book> findSimilarBooksAdvanced(Book book, int limit) {
        Map<Book, Integer> bookScores = new HashMap<>();
        List<Book> sameGenre = bookRepository.findByGenreAndIdNot(book.getGenre(), book.getId());
        for (Book b : sameGenre) bookScores.put(b, bookScores.getOrDefault(b, 0) + 3);
        List<Book> sameAuthor = bookRepository.findByAuthorAndIdNot(book.getAuthor(), book.getId());
        for (Book b : sameAuthor) bookScores.put(b, bookScores.getOrDefault(b, 0) + 2);
        if (book.getTitle() != null && !book.getTitle().isEmpty()) {
            String firstWord = book.getTitle().split(" ")[0];
            List<Book> similarTitle = bookRepository.findAll().stream()
                    .filter(b -> !b.getId().equals(book.getId()) && b.getTitle() != null &&
                            b.getTitle().toLowerCase().contains(firstWord.toLowerCase()))
                    .collect(Collectors.toList());
            for (Book b : similarTitle) bookScores.put(b, bookScores.getOrDefault(b, 0) + 1);
        }
        List<Book> sortedBooks = bookScores.entrySet().stream()
                .sorted(Map.Entry.<Book, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey).collect(Collectors.toList());
        if (sortedBooks.size() < limit) {
            int remaining = limit - sortedBooks.size();
            List<Book> allBooks = bookRepository.findAll();
            List<Book> availableBooks = allBooks.stream()
                    .filter(b -> !b.getId().equals(book.getId()) && !sortedBooks.contains(b))
                    .collect(Collectors.toList());
            Collections.shuffle(availableBooks);
            sortedBooks.addAll(availableBooks.stream().limit(remaining).collect(Collectors.toList()));
        }
        return sortedBooks.subList(0, Math.min(sortedBooks.size(), limit));
    }
}