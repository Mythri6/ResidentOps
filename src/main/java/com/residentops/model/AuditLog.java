package com.residentops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "performed_by", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User performedBy;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "performed_at")
    private LocalDateTime performedAt = LocalDateTime.now();
}
