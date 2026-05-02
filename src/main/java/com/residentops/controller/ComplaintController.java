package com.residentops.controller;

import com.residentops.model.enums.ComplaintStatus;
import com.residentops.model.enums.ComplaintType;
import com.residentops.service.ComplaintService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(complaintService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return complaintService.getById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/resident/{residentId}")
    public ResponseEntity<?> getByResident(@PathVariable Long residentId) {
        return ResponseEntity.ok(complaintService.getByResident(residentId));
    }

    @PostMapping("/file")
    public ResponseEntity<?> file(@RequestBody Map<String, String> body) {
        try {
            var c = complaintService.fileComplaint(
                    Long.parseLong(body.get("filedById")),
                    body.get("title"),
                    body.get("description"),
                    ComplaintType.valueOf(body.get("type")),
                    body.containsKey("againstUserId")
                            ? Long.parseLong(body.get("againstUserId")) : null
            );
            return ResponseEntity.ok(c);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        try {
            var c = complaintService.updateStatus(
                    id,
                    ComplaintStatus.valueOf(body.get("status")),
                    Long.parseLong(body.get("adminId")),
                    body.get("resolution")
            );
            return ResponseEntity.ok(c);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
