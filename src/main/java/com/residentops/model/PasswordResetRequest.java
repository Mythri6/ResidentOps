package com.residentops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Resident/Vendor password reset request sent to admin.
 * Admin resets to default RO@123 and notifies only the requester.
 */
@Entity
@Table(name = "password_reset_requests")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class PasswordResetRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User user;

    // PENDING | COMPLETED
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // Flag: user must change password on next action
    @Column(name = "force_change")
    private boolean forceChange = false;
}
