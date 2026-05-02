package com.residentops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.residentops.model.enums.VoteChoice;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "votes", uniqueConstraints = @UniqueConstraint(columnNames = {"poll_id","voted_by"}))
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "poll_id", nullable = false)
    @JsonIgnoreProperties({"createdBy", "description"})
    private Poll poll;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "voted_by", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User votedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteChoice choice;

    @Column(name = "voted_at")
    private LocalDateTime votedAt = LocalDateTime.now();
}
