package com.residentops.controller;

import com.residentops.singleton.AuditLogger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditLogger auditLogger;

    public AuditController(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(auditLogger.getAll());
    }

    @GetMapping("/entity/{type}")
    public ResponseEntity<?> getByEntityType(@PathVariable String type) {
        return ResponseEntity.ok(auditLogger.getByEntityType(type));
    }

    @GetMapping("/actor/{userId}")
    public ResponseEntity<?> getByActor(@PathVariable Long userId) {
        return ResponseEntity.ok(auditLogger.getByActor(userId));
    }
}
