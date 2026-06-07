package com.library.booksystem.repository;

import com.library.booksystem.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByGenreAndIdNot(String genre, Long id);
    List<Book> findByAuthorAndIdNot(String author, Long id);

    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Book> findByTitleContainingIgnoreCase(@Param("title") String title);

    // Получить последние добавленные книги (сортировка по createdAt DESC)
    List<Book> findTop6ByOrderByCreatedAtDesc();

    List<Book> findTop10ByOrderByCreatedAtDesc();
    // Получить книги по жанру с ограничением (для трендов)
    List<Book> findTop6ByGenreOrderByCreatedAtDesc(String genre);

    List<Book> findByGenre(String genre);

}