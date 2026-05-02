package com.residentops.controller;

import com.residentops.model.enums.Priority;
import com.residentops.model.enums.RequestCategory;
import com.residentops.model.enums.RequestStatus;
import com.residentops.service.MaintenanceService;
import com.residentops.service.SLAService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final SLAService slaService;

    public MaintenanceController(MaintenanceService maintenanceService, SLAService slaService) {
        this.maintenanceService = maintenanceService;
        this.slaService = slaService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(maintenanceService.getAllRequests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return maintenanceService.getById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/resident/{residentId}")
    public ResponseEntity<?> getByResident(@PathVariable Long residentId) {
        return ResponseEntity.ok(maintenanceService.getRequestsByResident(residentId));
    }

    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<?> getByVendor(@PathVariable Long vendorId) {
        return ResponseEntity.ok(maintenanceService.getRequestsByVendor(vendorId));
    }

    @PostMapping("/raise")
    public ResponseEntity<?> raiseRequest(@RequestBody Map<String, String> body) {
        try {
            var req = maintenanceService.raiseRequest(
                    Long.parseLong(body.get("residentId")),
                    body.get("title"),
                    body.get("description"),
                    RequestCategory.valueOf(body.get("category")),
                    Priority.valueOf(body.get("priority"))
            );
            return ResponseEntity.ok(req);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<?> assign(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            var req = maintenanceService.assignVendor(
                    id,
                    Long.parseLong(body.get("vendorId")),
                    Long.parseLong(body.get("adminId"))
            );
            return ResponseEntity.ok(req);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        try {
            var req = maintenanceService.updateStatus(
                    id,
                    RequestStatus.valueOf(body.get("status")),
                    Long.parseLong(body.get("actorId")),
                    body.get("workSummary")
            );
            return ResponseEntity.ok(req);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> closeRequest(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        try {
            var req = maintenanceService.closeRequest(
                    id,
                    Long.parseLong(body.get("adminId")),
                    body.get("workSummary"),
                    Double.parseDouble(body.getOrDefault("cost", "0"))
            );
            return ResponseEntity.ok(req);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id,
                                            @RequestBody Map<String, String> body) {
        try {
            var req = maintenanceService.rejectRequest(
                    id,
                    Long.parseLong(body.get("adminId")),
                    body.get("reason")
            );
            return ResponseEntity.ok(req);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<?> submitFeedback(@PathVariable Long id,
                                             @RequestBody Map<String, String> body) {
        try {
            var fb = maintenanceService.submitFeedback(
                    id,
                    Long.parseLong(body.get("residentId")),
                    Integer.parseInt(body.get("rating")),
                    body.get("comment")
            );
            return ResponseEntity.ok(fb);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sla/check")
    public ResponseEntity<?> triggerSLACheck(@RequestBody Map<String, String> body) {
        int count = slaService.runManualCheck();
        return ResponseEntity.ok(Map.of("escalated", count));
    }
}
