package com.residentops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.residentops.model.enums.ExpenseCategory;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approved_by", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User approvedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendor_id")
    @JsonIgnoreProperties({"password"})
    private User vendor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "request_id")
    @JsonIgnoreProperties({"raisedBy","assignedTo","description","workSummary"})
    private MaintenanceRequest relatedRequest;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
