package com.residentops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.residentops.model.enums.Priority;
import com.residentops.model.enums.RequestCategory;
import com.residentops.model.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * GRASP — Information Expert: MaintenanceRequest owns isSLABreached()
 * because it holds slaDeadline and status.
 */
@Entity
@Table(name = "maintenance_requests")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.RAISED;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "raised_by", nullable = false)
    @JsonIgnoreProperties({"password", "createdAt"})
    private User raisedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_to")
    @JsonIgnoreProperties({"password", "createdAt"})
    private User assignedTo;

    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "work_summary", columnDefinition = "TEXT")
    private String workSummary;

    @Column(name = "cost_incurred")
    private double costIncurred = 0.0;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** GRASP Information Expert */
    public boolean isSLABreached() {
        return slaDeadline != null
                && LocalDateTime.now().isAfter(slaDeadline)
                && status != RequestStatus.RESOLVED
                && status != RequestStatus.CLOSED;
    }

    public void updateStatus(RequestStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        if (newStatus == RequestStatus.CLOSED) {
            this.closedAt = LocalDateTime.now();
        }
    }
}
