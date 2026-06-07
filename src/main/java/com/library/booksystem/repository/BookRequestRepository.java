package com.library.booksystem.repository;

import com.library.booksystem.entity.BookRequest;
import com.library.booksystem.dto.BookRequestDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookRequestRepository extends JpaRepository<BookRequest, Long> {
    List<BookRequest> findByUserId(Long userId);
    List<BookRequest> findByBookId(Long bookId);
    List<BookRequest> findByStatus(String status);
    List<BookRequest> findByProcessedBy(Long processedBy);
    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, String status);

    @Query("SELECT new com.library.booksystem.dto.BookRequestDTO(" +
            "br.id, b.title, b.author, u.username, u.fullName, u.studentId, " +
            "u.faculty, COALESCE(br.requestDate, CURRENT_TIMESTAMP), br.pickupDate, br.returnDate, br.status, br.notes) " +
            "FROM BookRequest br " +
            "JOIN br.book b " +
            "JOIN br.user u " +
            "WHERE br.user.id = :userId")
    List<BookRequestDTO> findUserRequestsWithDetails(@Param("userId") Long userId);

    @Query("SELECT new com.library.booksystem.dto.BookRequestDTO(" +
            "br.id, b.title, b.author, u.username, u.fullName, u.studentId, " +
            "u.faculty, COALESCE(br.requestDate, CURRENT_TIMESTAMP), br.pickupDate, br.returnDate, br.status, br.notes) " +
            "FROM BookRequest br " +
            "JOIN br.book b " +
            "JOIN br.user u " +
            "WHERE br.status = :status " +
            "ORDER BY br.requestDate DESC")
    List<BookRequestDTO> findRequestsByStatusWithDetails(@Param("status") String status);

    @Query("SELECT COUNT(br) FROM BookRequest br WHERE br.book.id = :bookId")
    long countByBookId(@Param("bookId") Long bookId);
}