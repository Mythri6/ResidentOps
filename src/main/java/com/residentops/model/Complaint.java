package com.residentops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.residentops.model.enums.ComplaintStatus;
import com.residentops.model.enums.ComplaintType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "complaint_type", nullable = false)
    private ComplaintType complaintType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status = ComplaintStatus.FILED;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "filed_by", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User filedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "against_user")
    @JsonIgnoreProperties({"password"})
    private User againstUser;

    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
