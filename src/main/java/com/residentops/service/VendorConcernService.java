package com.residentops.service;

import com.residentops.model.User;
import com.residentops.model.VendorConcern;
import com.residentops.model.enums.VendorConcernStatus;
import com.residentops.model.enums.VendorConcernType;
import com.residentops.repository.MaintenanceRequestRepository;
import com.residentops.repository.UserRepository;
import com.residentops.repository.VendorConcernRepository;
import com.residentops.singleton.AuditLogger;
import com.residentops.singleton.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VendorConcernService {

    private final VendorConcernRepository concernRepository;
    private final UserRepository userRepository;
    private final MaintenanceRequestRepository requestRepository;
    private final NotificationService notificationService;
    private final AuditLogger auditLogger;

    public VendorConcernService(VendorConcernRepository concernRepository,
                                 UserRepository userRepository,
                                 MaintenanceRequestRepository requestRepository,
                                 NotificationService notificationService,
                                 AuditLogger auditLogger) {
        this.concernRepository = concernRepository;
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.notificationService = notificationService;
        this.auditLogger = auditLogger;
    }

    // ── VENDOR: Raise a concern ────────────────────────────────
    @Transactional
    public VendorConcern raiseConcern(Long vendorId, String concernType,
                                       Long relatedRequestId, String description,
                                       LocalDate leaveFrom, LocalDate leaveTo,
                                       String leaveReason) {
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        VendorConcern concern = new VendorConcern();
        concern.setVendor(vendor);
        concern.setConcernType(VendorConcernType.valueOf(concernType));
        concern.setDescription(description);

        if (relatedRequestId != null) {
            requestRepository.findById(relatedRequestId)
                    .ifPresent(concern::setRelatedRequest);
        }
        if (VendorConcernType.LEAVE.name().equals(concernType)) {
            concern.setLeaveFrom(leaveFrom);
            concern.setLeaveTo(leaveTo);
            concern.setLeaveReason(leaveReason);
        }
        VendorConcern saved = concernRepository.save(concern);
        notificationService.notifyAll("VENDOR_CONCERN_RAISED",
                vendor.getName() + " raised a concern: " + concernType, saved.getId());
        auditLogger.log("VENDOR_CONCERN_RAISED", "VendorConcern",
                saved.getId(), vendorId, concernType);
        return saved;
    }

    // ── ADMIN: Approve (works for leave + work_related) ────────
    @Transactional
    public VendorConcern approveConcern(Long concernId, Long adminId, String message) {
        VendorConcern c = findOrThrow(concernId);
        c.setStatus(VendorConcernStatus.APPROVED);
        c.setAdminMessage(message);
        c.setProcessedAt(LocalDateTime.now());
        VendorConcern saved = concernRepository.save(c);
        notificationService.notifyAll("CONCERN_APPROVED",
                "Your concern #" + concernId + " has been approved.",
                c.getVendor().getId());
        auditLogger.log("CONCERN_APPROVED", "VendorConcern", concernId, adminId, message);
        return saved;
    }

    // ── ADMIN: Reject / Drop ───────────────────────────────────
    @Transactional
    public VendorConcern rejectConcern(Long concernId, Long adminId, String message) {
        VendorConcern c = findOrThrow(concernId);
        c.setStatus(VendorConcernStatus.REJECTED);
        c.setAdminMessage(message);
        c.setProcessedAt(LocalDateTime.now());
        VendorConcern saved = concernRepository.save(c);
        notificationService.notifyAll("CONCERN_REJECTED",
                "Your concern #" + concernId + " was rejected. " + message,
                c.getVendor().getId());
        auditLogger.log("CONCERN_REJECTED", "VendorConcern", concernId, adminId, message);
        return saved;
    }

    @Transactional
    public VendorConcern dropConcern(Long concernId, Long adminId, String message) {
        VendorConcern c = findOrThrow(concernId);
        c.setStatus(VendorConcernStatus.DROPPED);
        c.setAdminMessage(message);
        c.setProcessedAt(LocalDateTime.now());
        VendorConcern saved = concernRepository.save(c);
        auditLogger.log("CONCERN_DROPPED", "VendorConcern", concernId, adminId, message);
        return saved;
    }

    // ── Check if vendor is on approved leave ──────────────────
    public boolean isVendorOnLeave(Long vendorId) {
        User vendor = userRepository.findById(vendorId).orElse(null);
        if (vendor == null) return false;
        List<VendorConcern> approved = concernRepository
                .findByVendorAndConcernTypeAndStatus(vendor,
                        VendorConcernType.LEAVE, VendorConcernStatus.APPROVED);
        return approved.stream().anyMatch(VendorConcern::isOnLeaveToday);
    }

    /** Returns the active leave concern for a vendor if on leave today, else null */
    public VendorConcern getActiveLeave(Long vendorId) {
        User vendor = userRepository.findById(vendorId).orElse(null);
        if (vendor == null) return null;
        return concernRepository
                .findByVendorAndConcernTypeAndStatus(vendor,
                        VendorConcernType.LEAVE, VendorConcernStatus.APPROVED)
                .stream()
                .filter(VendorConcern::isOnLeaveToday)
                .findFirst().orElse(null);
    }

    public List<VendorConcern> getAll()                       { return concernRepository.findAllByOrderByCreatedAtDesc(); }
    public List<VendorConcern> getPending()                   { return concernRepository.findByStatus(VendorConcernStatus.PENDING); }
    public List<VendorConcern> getByVendor(Long vendorId) {
        User v = userRepository.findById(vendorId).orElseThrow(() -> new RuntimeException("Vendor not found"));
        return concernRepository.findByVendor(v);
    }
    public Optional<VendorConcern> getById(Long id)          { return concernRepository.findById(id); }

    private VendorConcern findOrThrow(Long id) {
        return concernRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Concern not found: " + id));
    }
}
