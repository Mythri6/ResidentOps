package com.residentops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.residentops.model.enums.OwnerType;
import com.residentops.model.enums.Role;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // ── Resident fields ──────────────────
    @Column(name = "apartment_no", length = 20)
    private String apartmentNo;

    @Column(name = "block_no", length = 10)
    private String blockNo;

    @Column(name = "floor_no", length = 10)
    private String floorNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type")
    private OwnerType ownerType;

    @Column(name = "phone_no", length = 15)
    private String phoneNo;

    // ── Vendor fields ────────────────────
    @Column(name = "service_type", length = 100)
    private String serviceType;

    @Column(nullable = false)
    private double rating = 0.0;

    // ── Account management ───────────────
    @Column(name = "force_password_change", nullable = false)
    private boolean forcePasswordChange = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }
}
