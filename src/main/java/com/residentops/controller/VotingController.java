package com.residentops.controller;

import com.residentops.model.enums.VoteChoice;
import com.residentops.service.VotingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/polls")
@CrossOrigin(origins = "*")
public class VotingController {

    private final VotingService votingService;

    public VotingController(VotingService votingService) {
        this.votingService = votingService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(votingService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return votingService.getById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<?> getResults(@PathVariable Long id) {
        return ResponseEntity.ok(votingService.getPollResults(id));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createPoll(@RequestBody Map<String, String> body) {
        try {
            var poll = votingService.createPoll(
                    Long.parseLong(body.get("adminId")),
                    body.get("question"),
                    body.get("description"),
                    Integer.parseInt(body.getOrDefault("quorum", "5")),
                    LocalDateTime.parse(body.get("deadline"))
            );
            return ResponseEntity.ok(poll);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<?> castVote(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        try {
            var vote = votingService.castVote(
                    id,
                    Long.parseLong(body.get("userId")),
                    VoteChoice.valueOf(body.get("choice"))
            );
            return ResponseEntity.ok(vote);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> closePoll(@PathVariable Long id,
                                        @RequestBody Map<String, String> body) {
        try {
            var poll = votingService.closePoll(id, Long.parseLong(body.get("adminId")));
            return ResponseEntity.ok(poll);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
