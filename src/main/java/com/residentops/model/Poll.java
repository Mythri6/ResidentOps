package com.residentops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.residentops.model.enums.PollStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "polls")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User createdBy;

    @Column(nullable = false)
    private int quorum = 5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PollStatus status = PollStatus.OPEN;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(length = 200)
    private String result;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** GRASP Information Expert — Poll owns deadline check */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(deadline);
    }
}
