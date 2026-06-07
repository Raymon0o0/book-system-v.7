package com.library.booksystem.dto;

import java.time.LocalDateTime;

public class BookRequestDTO {
    private Long id;
    private String bookTitle;
    private String bookAuthor;
    private String userName;
    private String userFullName;
    private String userStudentId;
    private String userFaculty;
    private LocalDateTime requestDate;
    private LocalDateTime pickupDate;
    private LocalDateTime returnDate;
    private String status;
    private String notes;

    // Конструкторы
    public BookRequestDTO() {}

    public BookRequestDTO(Long id, String bookTitle, String bookAuthor,
                          String userName, String userFullName, String userStudentId,
                          String userFaculty, LocalDateTime requestDate,
                          LocalDateTime pickupDate, LocalDateTime returnDate,
                          String status, String notes) {
        this.id = id;
        this.bookTitle = bookTitle;
        this.bookAuthor = bookAuthor;
        this.userName = userName;
        this.userFullName = userFullName;
        this.userStudentId = userStudentId;
        this.userFaculty = userFaculty;
        this.requestDate = requestDate;
        this.pickupDate = pickupDate;
        this.returnDate = returnDate;
        this.status = status;
        this.notes = notes;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getBookAuthor() { return bookAuthor; }
    public void setBookAuthor(String bookAuthor) { this.bookAuthor = bookAuthor; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public String getUserStudentId() { return userStudentId; }
    public void setUserStudentId(String userStudentId) { this.userStudentId = userStudentId; }

    public String getUserFaculty() { return userFaculty; }
    public void setUserFaculty(String userFaculty) { this.userFaculty = userFaculty; }

    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }

    public LocalDateTime getPickupDate() { return pickupDate; }
    public void setPickupDate(LocalDateTime pickupDate) { this.pickupDate = pickupDate; }

    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}