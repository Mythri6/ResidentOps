package com.residentops.service;

import com.residentops.model.Expense;
import com.residentops.model.User;
import com.residentops.model.enums.ExpenseCategory;
import com.residentops.repository.ExpenseRepository;
import com.residentops.repository.MaintenanceRequestRepository;
import com.residentops.repository.UserRepository;
import com.residentops.singleton.AuditLogger;
import com.residentops.singleton.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final MaintenanceRequestRepository requestRepository;
    private final AuditLogger auditLogger;
    private final NotificationService notificationService;

    public ExpenseService(ExpenseRepository expenseRepository, UserRepository userRepository,
                          MaintenanceRequestRepository requestRepository,
                          AuditLogger auditLogger, NotificationService notificationService) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.auditLogger = auditLogger;
        this.notificationService = notificationService;
    }

    @Transactional
    public Expense addExpense(Long adminId, String title, double amount, ExpenseCategory category,
                              Long vendorId, Long requestId, String notes, LocalDate date) {
        User admin = userRepository.findById(adminId).orElseThrow(() -> new RuntimeException("Admin not found"));
        Expense e = new Expense();
        e.setTitle(title); e.setAmount(amount); e.setCategory(category);
        e.setApprovedBy(admin); e.setNotes(notes); e.setExpenseDate(date);
        if (vendorId != null) userRepository.findById(vendorId).ifPresent(e::setVendor);
        if (requestId != null) requestRepository.findById(requestId).ifPresent(e::setRelatedRequest);
        Expense saved = expenseRepository.save(e);
        notificationService.notifyAll("EXPENSE_ADDED", "Expense ₹" + amount + " for " + title, saved.getId());
        auditLogger.log("EXPENSE_ADDED", "Expense", saved.getId(), adminId, "Amount: " + amount);
        return saved;
    }

    public List<Expense> getAll() { return expenseRepository.findAllByOrderByCreatedAtDesc(); }
    public Optional<Expense> getById(Long id) { return expenseRepository.findById(id); }
    public Double getTotalExpenses() { Double t = expenseRepository.getTotalExpenses(); return t != null ? t : 0.0; }
}
