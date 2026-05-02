package com.residentops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.residentops.model.enums.FlatUpdateStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Resident's request to add a new member to their flat.
 * Admin must verify the Aadhaar proof before approving.
 */
@Entity
@Table(name = "flat_update_requests")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class FlatUpdateRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resident_id", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User resident;

    @Column(name = "new_member_name", nullable = false, length = 100)
    private String newMemberName;

    @Column(name = "new_member_phone", length = 15)
    private String newMemberPhone;

    @Column(name = "proof_filename", length = 255)
    private String proofFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlatUpdateStatus status = FlatUpdateStatus.UNDER_VERIFICATION;

    @Column(name = "admin_remarks", columnDefinition = "TEXT")
    private String adminRemarks;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
