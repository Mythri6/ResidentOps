package com.residentops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.residentops.model.enums.NoticeType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_type", nullable = false)
    private NoticeType noticeType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "posted_by", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User postedBy;

    @Column(name = "is_emergency", nullable = false)
    private boolean emergency = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
