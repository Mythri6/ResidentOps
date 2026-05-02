package com.residentops.service;

import com.residentops.model.Complaint;
import com.residentops.model.User;
import com.residentops.model.enums.ComplaintStatus;
import com.residentops.model.enums.ComplaintType;
import com.residentops.repository.ComplaintRepository;
import com.residentops.repository.UserRepository;
import com.residentops.singleton.AuditLogger;
import com.residentops.singleton.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogger auditLogger;

    public ComplaintService(ComplaintRepository complaintRepository,
                            UserRepository userRepository,
                            NotificationService notificationService,
                            AuditLogger auditLogger) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public Complaint fileComplaint(Long filedById, String title, String description,
                                   ComplaintType type, Long againstUserId) {
        User filer = userRepository.findById(filedById)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Complaint c = new Complaint();
        c.setTitle(title);
        c.setDescription(description);
        c.setComplaintType(type);
        c.setFiledBy(filer);

        if (againstUserId != null) {
            userRepository.findById(againstUserId).ifPresent(c::setAgainstUser);
        }

        Complaint saved = complaintRepository.save(c);
        notificationService.notifyAll("COMPLAINT_FILED",
                "New complaint: " + title, saved.getId());
        auditLogger.log("COMPLAINT_FILED", "Complaint",
                saved.getId(), filedById, type.name());
        return saved;
    }

    @Transactional
    public Complaint updateStatus(Long complaintId, ComplaintStatus newStatus,
                                  Long adminId, String resolution) {
        Complaint c = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
        c.setStatus(newStatus);
        if (resolution != null) c.setResolution(resolution);
        if (newStatus == ComplaintStatus.RESOLVED || newStatus == ComplaintStatus.CLOSED) {
            c.setResolvedAt(LocalDateTime.now());
        }
        Complaint saved = complaintRepository.save(c);
        auditLogger.log("COMPLAINT_STATUS_UPDATED", "Complaint",
                complaintId, adminId, "Status → " + newStatus);
        return saved;
    }

    public List<Complaint> getAll() {
        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Complaint> getById(Long id) {
        return complaintRepository.findById(id);
    }

    public List<Complaint> getByResident(Long residentId) {
        User u = userRepository.findById(residentId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return complaintRepository.findByFiledBy(u);
    }
}
