package com.residentops.controller;

import com.residentops.model.enums.ExpenseCategory;
import com.residentops.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(expenseService.getAll());
    }

    @GetMapping("/total")
    public ResponseEntity<?> getTotal() {
        return ResponseEntity.ok(Map.of("total", expenseService.getTotalExpenses()));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addExpense(@RequestBody Map<String, String> body) {
        try {
            var exp = expenseService.addExpense(
                    Long.parseLong(body.get("adminId")),
                    body.get("title"),
                    Double.parseDouble(body.get("amount")),
                    ExpenseCategory.valueOf(body.get("category")),
                    body.containsKey("vendorId") ? Long.parseLong(body.get("vendorId")) : null,
                    body.containsKey("requestId") ? Long.parseLong(body.get("requestId")) : null,
                    body.get("notes"),
                    LocalDate.parse(body.get("expenseDate"))
            );
            return ResponseEntity.ok(exp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
