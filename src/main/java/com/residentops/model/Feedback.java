package com.residentops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "request_id", nullable = false)
    @JsonIgnoreProperties({"raisedBy","assignedTo","description","workSummary"})
    private MaintenanceRequest request;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "given_by", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User givenBy;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
