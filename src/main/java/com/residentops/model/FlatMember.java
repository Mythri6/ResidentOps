package com.residentops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents one person living in a flat (resident's family member).
 * Linked to the flat's User account.
 */
@Entity
@Table(name = "flat_members")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class FlatMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User resident;

    @Column(nullable = false, length = 100)
    private String memberName;

    @Column(length = 15)
    private String phoneNo;
}
