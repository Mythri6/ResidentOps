package com.residentops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.residentops.model.enums.VendorConcernStatus;
import com.residentops.model.enums.VendorConcernType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vendor concern / leave request raised by a vendor.
 * Admin processes it; leave approval blocks vendor assignment.
 */
@Entity
@Table(name = "vendor_concerns")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class VendorConcern {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendor_id", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "concern_type", nullable = false)
    private VendorConcernType concernType;

    // For WORK_RELATED — which request
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "related_request_id")
    @JsonIgnoreProperties({"raisedBy","assignedTo","description","workSummary"})
    private MaintenanceRequest relatedRequest;

    // Description (required for OTHERS, optional for WORK_RELATED)
    @Column(columnDefinition = "TEXT")
    private String description;

    // Leave fields
    @Column(name = "leave_from")
    private LocalDate leaveFrom;

    @Column(name = "leave_to")
    private LocalDate leaveTo;

    @Column(name = "leave_reason", columnDefinition = "TEXT")
    private String leaveReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VendorConcernStatus status = VendorConcernStatus.PENDING;

    @Column(name = "admin_message", columnDefinition = "TEXT")
    private String adminMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /** True if vendor is on approved leave today */
    public boolean isOnLeaveToday() {
        if (concernType != VendorConcernType.LEAVE) return false;
        if (status != VendorConcernStatus.APPROVED) return false;
        LocalDate today = LocalDate.now();
        return (leaveFrom != null && leaveTo != null)
                && !today.isBefore(leaveFrom) && !today.isAfter(leaveTo);
    }
}
