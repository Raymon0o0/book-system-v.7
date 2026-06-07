package com.library.booksystem.controller;

import com.library.booksystem.entity.Book;
import com.library.booksystem.entity.BookRequest;
import com.library.booksystem.entity.User;
import com.library.booksystem.dto.BookRequestDTO;
import com.library.booksystem.repository.BookRepository;
import com.library.booksystem.repository.BookRequestRepository;
import com.library.booksystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/requests")
@Transactional
public class BookRequestController {

    @Autowired
    private BookRequestRepository bookRequestRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/my")
    public String showMyRequests(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        List<BookRequestDTO> requests = bookRequestRepository.findUserRequestsWithDetails(user.getId());
        model.addAttribute("requests", requests);
        return "my-requests";
    }

    @PostMapping("/create/{bookId}")
    public String createRequest(@PathVariable Long bookId,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isEmpty()) {
            referer = "/";
        }
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (!bookOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Книга не найдена");
                return "redirect:" + referer;
            }

            Book book = bookOpt.get();

            boolean exists = bookRequestRepository.existsByUserIdAndBookIdAndStatus(
                    user.getId(), bookId, "PENDING");

            if (exists) {
                redirectAttributes.addFlashAttribute("error", "У вас уже есть активная заявка на эту книгу");
                return "redirect:" + referer;
            }

            if (book.getCopies() <= 0) {
                redirectAttributes.addFlashAttribute("error", "Нет доступных копий этой книги");
                return "redirect:" + referer;
            }

            BookRequest bookRequest = new BookRequest(user, book);
            bookRequestRepository.save(bookRequest);

            redirectAttributes.addFlashAttribute("success", "Заявка успешно создана");
            return "redirect:" + referer;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при создании заявки: " + e.getMessage());
            return "redirect:" + referer;
        }
    }


    @GetMapping("/manage")
    public String manageRequests(Model model) {
        List<BookRequestDTO> pendingRequests = bookRequestRepository.findRequestsByStatusWithDetails("PENDING");
        List<BookRequestDTO> approvedRequests = bookRequestRepository.findRequestsByStatusWithDetails("APPROVED");
        List<BookRequestDTO> borrowedRequests = bookRequestRepository.findRequestsByStatusWithDetails("BORROWED");
        List<BookRequestDTO> returnedRequests = bookRequestRepository.findRequestsByStatusWithDetails("RETURNED");
        List<BookRequestDTO> rejectedRequests = bookRequestRepository.findRequestsByStatusWithDetails("REJECTED");

        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("approvedRequests", approvedRequests);
        model.addAttribute("borrowedRequests", borrowedRequests);
        model.addAttribute("returnedRequests", returnedRequests);
        model.addAttribute("rejectedRequests", rejectedRequests);
        return "manage-requests";
    }

    @PostMapping("/approve/{requestId}")
    public String approveRequest(@PathVariable Long requestId,
                                 @RequestParam(required = false) String notes,
                                 RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User librarian = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            Optional<BookRequest> requestOpt = bookRequestRepository.findById(requestId);
            if (!requestOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Заявка не найдена");
                return "redirect:/requests/manage";
            }

            BookRequest request = requestOpt.get();
            Book book = request.getBook();

            if (book.getCopies() <= 0) {
                redirectAttributes.addFlashAttribute("error", "Нет доступных копий книги");
                return "redirect:/requests/manage";
            }

            book.setCopies(book.getCopies() - 1);
            bookRepository.save(book);

            request.setStatus("APPROVED");
            request.setProcessedBy(librarian.getId());
            request.setProcessedDate(LocalDateTime.now());
            request.setPickupDate(LocalDateTime.now().plusDays(1));
            request.setNotes(notes);
            bookRequestRepository.save(request);

            redirectAttributes.addFlashAttribute("success", "Заявка одобрена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при одобрении заявки: " + e.getMessage());
        }
        return "redirect:/requests/manage";
    }

    @PostMapping("/reject/{requestId}")
    public String rejectRequest(@PathVariable Long requestId,
                                @RequestParam(required = false) String notes,
                                RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User librarian = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            Optional<BookRequest> requestOpt = bookRequestRepository.findById(requestId);
            if (!requestOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Заявка не найдена");
                return "redirect:/requests/manage";
            }

            BookRequest request = requestOpt.get();
            request.setStatus("REJECTED");
            request.setProcessedBy(librarian.getId());
            request.setProcessedDate(LocalDateTime.now());
            request.setNotes(notes);
            bookRequestRepository.save(request);

            redirectAttributes.addFlashAttribute("success", "Заявка отклонена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при отклонении заявки: " + e.getMessage());
        }
        return "redirect:/requests/manage";
    }

    @PostMapping("/issue/{requestId}")
    public String issueBook(@PathVariable Long requestId,
                            RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User librarian = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            Optional<BookRequest> requestOpt = bookRequestRepository.findById(requestId);
            if (!requestOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Заявка не найдена");
                return "redirect:/requests/manage";
            }

            BookRequest request = requestOpt.get();

            if (!"APPROVED".equals(request.getStatus())) {
                redirectAttributes.addFlashAttribute("error", "Заявка должна быть одобрена перед выдачей");
                return "redirect:/requests/manage";
            }

            request.setStatus("BORROWED");
            request.setPickupDate(LocalDateTime.now());
            request.setReturnDate(LocalDateTime.now().plusDays(14));
            request.setProcessedBy(librarian.getId());
            request.setProcessedDate(LocalDateTime.now());
            bookRequestRepository.save(request);

            redirectAttributes.addFlashAttribute("success", "Книга успешно выдана");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при выдаче книги: " + e.getMessage());
        }
        return "redirect:/requests/manage";
    }

    @PostMapping("/return/{requestId}")
    public String returnBook(@PathVariable Long requestId,
                             RedirectAttributes redirectAttributes) {
        try {
            Optional<BookRequest> requestOpt = bookRequestRepository.findById(requestId);
            if (!requestOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Заявка не найдена");
                return "redirect:/requests/manage";
            }

            BookRequest request = requestOpt.get();

            if (!"BORROWED".equals(request.getStatus())) {
                redirectAttributes.addFlashAttribute("error", "Книга не была выдана");
                return "redirect:/requests/manage";
            }

            Book book = request.getBook();
            book.setCopies(book.getCopies() + 1);
            bookRepository.save(book);

            request.setStatus("RETURNED");
            request.setReturnDate(LocalDateTime.now());
            bookRequestRepository.save(request);

            redirectAttributes.addFlashAttribute("success", "Книга успешно возвращена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при возврате книги: " + e.getMessage());
        }
        return "redirect:/requests/manage";
    }

    @PostMapping("/cancel/{requestId}")
    public String cancelRequest(@PathVariable Long requestId,
                                RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            Optional<BookRequest> requestOpt = bookRequestRepository.findById(requestId);
            if (!requestOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Заявка не найдена");
                return "redirect:/requests/my";
            }

            BookRequest request = requestOpt.get();
            if (!request.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Вы не можете отменить чужую заявку");
                return "redirect:/requests/my";
            }

            if (!"PENDING".equals(request.getStatus())) {
                redirectAttributes.addFlashAttribute("error", "Можно отменить только заявки в статусе 'Ожидание'");
                return "redirect:/requests/my";
            }

            request.setStatus("CANCELLED");
            request.setNotes("Отменено пользователем");
            bookRequestRepository.save(request);

            redirectAttributes.addFlashAttribute("success", "Заявка успешно отменена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при отмене заявки: " + e.getMessage());
        }
        return "redirect:/requests/my";
    }
}