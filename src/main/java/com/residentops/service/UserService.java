package com.residentops.service;

import com.residentops.model.*;
import com.residentops.model.enums.*;
import com.residentops.repository.*;
import com.residentops.singleton.AuditLogger;
import com.residentops.singleton.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final FlatMemberRepository flatMemberRepository;
    private final FlatUpdateRequestRepository flatUpdateRequestRepository;
    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final AuditLogger auditLogger;
    private final NotificationService notificationService;

    public static final String DEFAULT_PASSWORD = "RO@123";

    public UserService(UserRepository userRepository,
                       FlatMemberRepository flatMemberRepository,
                       FlatUpdateRequestRepository flatUpdateRequestRepository,
                       PasswordResetRequestRepository passwordResetRequestRepository,
                       AuditLogger auditLogger,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.flatMemberRepository = flatMemberRepository;
        this.flatUpdateRequestRepository = flatUpdateRequestRepository;
        this.passwordResetRequestRepository = passwordResetRequestRepository;
        this.auditLogger = auditLogger;
        this.notificationService = notificationService;
    }

    // ── LOGIN ───────────────────────────────────────────────────
    public Optional<User> login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(u -> u.getPassword().equals(password));
    }

    // ── REGISTER RESIDENT (admin only) ─────────────────────────
    @Transactional
    public User registerResident(String name, String blockNo, String floorNo,
                                  String flatNo, String phoneNo, String ownerTypeStr,
                                  String apartmentPrefix) {
        if (userRepository.existsByApartmentNo(flatNo)) {
            throw new RuntimeException("Flat " + flatNo + " already has an account. Cannot assign again.");
        }
        String emailLocal = (apartmentPrefix + "B" + blockNo + "F" + floorNo + "R" + flatNo).toUpperCase();
        String email = emailLocal + "@resident.com";
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email " + email + " already in use.");
        }
        User u = new User(name, email, DEFAULT_PASSWORD, Role.RESIDENT);
        u.setApartmentNo(flatNo);
        u.setBlockNo(blockNo);
        u.setFloorNo(floorNo);
        u.setPhoneNo(phoneNo);
        u.setOwnerType(ownerTypeStr != null ? OwnerType.valueOf(ownerTypeStr) : OwnerType.OWNER);
        u.setForcePasswordChange(true);
        User saved = userRepository.save(u);
        auditLogger.log("RESIDENT_REGISTERED", "User", saved.getId(), saved.getId(),
                "Flat: " + flatNo + ", Email: " + email);
        return saved;
    }

    // ── REGISTER VENDOR (admin only) ──────────────────────────
    @Transactional
    public User registerVendor(String name, String phoneNo, String serviceType) {
        String localPart = name.trim().toLowerCase().replaceAll("\\s+", "");
        String email = localPart + "@vendor.com";
        int counter = 1;
        while (userRepository.existsByEmail(email)) {
            email = localPart + counter + "@vendor.com";
            counter++;
        }
        User u = new User(name, email, DEFAULT_PASSWORD, Role.VENDOR);
        u.setPhoneNo(phoneNo);
        u.setServiceType(serviceType);
        u.setForcePasswordChange(true);
        User saved = userRepository.save(u);
        auditLogger.log("VENDOR_REGISTERED", "User", saved.getId(), saved.getId(),
                "Service: " + serviceType + ", Email: " + email);
        return saved;
    }

    // ── CHANGE PASSWORD ────────────────────────────────────────
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!u.getPassword().equals(currentPassword)) {
            throw new RuntimeException("WRONG_CURRENT_PASSWORD");
        }
        if (newPassword == null || newPassword.length() < 4) {
            throw new RuntimeException("New password must be at least 4 characters.");
        }
        u.setPassword(newPassword);
        u.setForcePasswordChange(false);
        userRepository.save(u);
        auditLogger.log("PASSWORD_CHANGED", "User", userId, userId, "Self-change");
    }

    // ── FORGOT PASSWORD REQUEST (in-app) ───────────────────────
    @Transactional
    public PasswordResetRequest requestPasswordReset(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        passwordResetRequestRepository.findByUserAndStatus(u, "PENDING")
                .forEach(r -> { r.setStatus("CANCELLED"); passwordResetRequestRepository.save(r); });
        PasswordResetRequest req = new PasswordResetRequest();
        req.setUser(u);
        req.setStatus("PENDING");
        return passwordResetRequestRepository.save(req);
    }

    // ── ADMIN: RESET PASSWORD TO DEFAULT ──────────────────────
    @Transactional
    public void adminResetPassword(Long requestId, Long adminId) {
        PasswordResetRequest req = passwordResetRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        User u = req.getUser();
        u.setPassword(DEFAULT_PASSWORD);
        u.setForcePasswordChange(true);
        userRepository.save(u);
        req.setStatus("COMPLETED");
        req.setForceChange(true);
        req.setProcessedAt(LocalDateTime.now());
        passwordResetRequestRepository.save(req);
        // Notify ONLY the requester — not everyone
        notificationService.notifyAll("PASSWORD_RESET",
                "Your password has been reset to default (RO@123). Log in and change it immediately.",
                u.getId());
        auditLogger.log("PASSWORD_RESET_BY_ADMIN", "User", u.getId(), adminId,
                "Reset to default: " + u.getEmail());
    }

    // ── UPDATE PHONE ──────────────────────────────────────────
    @Transactional
    public void updatePhone(Long userId, String newPhone) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        u.setPhoneNo(newPhone);
        userRepository.save(u);
        auditLogger.log("PHONE_UPDATED", "User", userId, userId, "New: " + newPhone);
    }

    // ── FLAT MEMBER REQUESTS ───────────────────────────────────
    public List<FlatMember> getMembersForResident(Long residentId) {
        User u = userRepository.findById(residentId).orElseThrow(() -> new RuntimeException("Not found"));
        return flatMemberRepository.findByResident(u);
    }

    public long getMemberCount(Long residentId) {
        User u = userRepository.findById(residentId).orElseThrow(() -> new RuntimeException("Not found"));
        return flatMemberRepository.countByResident(u);
    }

    @Transactional
    public FlatUpdateRequest submitFlatUpdateRequest(Long residentId, String newMemberName,
                                                      String newMemberPhone, String proofFilename) {
        User u = userRepository.findById(residentId).orElseThrow(() -> new RuntimeException("Not found"));
        FlatUpdateRequest req = new FlatUpdateRequest();
        req.setResident(u);
        req.setNewMemberName(newMemberName);
        req.setNewMemberPhone(newMemberPhone);
        req.setProofFilename(proofFilename);
        req.setStatus(FlatUpdateStatus.UNDER_VERIFICATION);
        FlatUpdateRequest saved = flatUpdateRequestRepository.save(req);
        auditLogger.log("FLAT_UPDATE_REQUESTED", "FlatUpdateRequest", saved.getId(), residentId,
                "Adding: " + newMemberName);
        return saved;
    }

    public List<FlatUpdateRequest> getPendingFlatUpdateRequests() {
        return flatUpdateRequestRepository.findByStatus(FlatUpdateStatus.UNDER_VERIFICATION);
    }

    public List<FlatUpdateRequest> getAllFlatUpdateRequests() {
        return flatUpdateRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void approveFlatUpdate(Long requestId, Long adminId) {
        FlatUpdateRequest req = flatUpdateRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        FlatMember member = new FlatMember();
        member.setResident(req.getResident());
        member.setMemberName(req.getNewMemberName());
        member.setPhoneNo(req.getNewMemberPhone());
        flatMemberRepository.save(member);
        req.setStatus(FlatUpdateStatus.APPROVED);
        req.setProcessedAt(LocalDateTime.now());
        flatUpdateRequestRepository.save(req);
        notificationService.notifyAll("FLAT_MEMBER_ADDED",
                "Your flat member update has been approved. " + req.getNewMemberName() + " added.",
                req.getResident().getId());
        auditLogger.log("FLAT_UPDATE_APPROVED", "FlatUpdateRequest", requestId, adminId,
                "Approved for flat: " + req.getResident().getApartmentNo());
    }

    @Transactional
    public void rejectFlatUpdate(Long requestId, Long adminId, String remarks) {
        FlatUpdateRequest req = flatUpdateRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        req.setStatus(FlatUpdateStatus.REJECTED);
        req.setAdminRemarks(remarks);
        req.setProcessedAt(LocalDateTime.now());
        flatUpdateRequestRepository.save(req);
        notificationService.notifyAll("FLAT_UPDATE_REJECTED",
                "Your flat member update was rejected. Reason: " + remarks,
                req.getResident().getId());
        auditLogger.log("FLAT_UPDATE_REJECTED", "FlatUpdateRequest", requestId, adminId, remarks);
    }

    public List<PasswordResetRequest> getPendingResetRequests() {
        return passwordResetRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    public List<PasswordResetRequest> getAllResetRequests() {
        return passwordResetRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<User> getAll()             { return userRepository.findAll(); }
    public List<User> getByRole(Role role) { return userRepository.findByRole(role); }
    public Optional<User> getById(Long id) { return userRepository.findById(id); }
}
