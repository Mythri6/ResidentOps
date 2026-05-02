package com.residentops.controller;

import com.residentops.model.enums.NoticeType;
import com.residentops.service.NoticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/notices")
@CrossOrigin(origins = "*")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(noticeService.getAll());
    }

    @GetMapping("/emergencies")
    public ResponseEntity<?> getEmergencies() {
        return ResponseEntity.ok(noticeService.getEmergencies());
    }

    @PostMapping("/post")
    public ResponseEntity<?> postNotice(@RequestBody Map<String, String> body) {
        try {
            var notice = noticeService.postNotice(
                    Long.parseLong(body.get("adminId")),
                    body.get("title"),
                    body.get("body"),
                    NoticeType.valueOf(body.get("type")),
                    Boolean.parseBoolean(body.getOrDefault("emergency", "false"))
            );
            return ResponseEntity.ok(notice);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
