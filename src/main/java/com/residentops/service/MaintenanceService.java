package com.residentops.service;

import com.residentops.factory.RequestFactory;
import com.residentops.model.Feedback;
import com.residentops.model.MaintenanceRequest;
import com.residentops.model.User;
import com.residentops.model.enums.Priority;
import com.residentops.model.enums.RequestCategory;
import com.residentops.model.enums.RequestStatus;
import com.residentops.repository.FeedbackRepository;
import com.residentops.repository.MaintenanceRequestRepository;
import com.residentops.repository.UserRepository;
import com.residentops.singleton.AuditLogger;
import com.residentops.singleton.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * SOLID — SRP: Handles maintenance request lifecycle ONLY.
 * Uses RequestFactory (Creator) for object construction.
 * Delegates audit to AuditLogger singleton.
 * Delegates notifications to NotificationService singleton.
 */
@Service
public class MaintenanceService {

    private final MaintenanceRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final RequestFactory requestFactory;
    private final NotificationService notificationService;
    private final AuditLogger auditLogger;

    public MaintenanceService(MaintenanceRequestRepository requestRepository,
                              UserRepository userRepository,
                              FeedbackRepository feedbackRepository,
                              RequestFactory requestFactory,
                              NotificationService notificationService,
                              AuditLogger auditLogger) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.feedbackRepository = feedbackRepository;
        this.requestFactory = requestFactory;
        this.notificationService = notificationService;
        this.auditLogger = auditLogger;
    }

    /** RAISE — Resident creates a new request */
    @Transactional
    public MaintenanceRequest raiseRequest(Long residentId, String title,
                                           String description,
                                           RequestCategory category,
                                           Priority priority) {
        User resident = userRepository.findById(residentId)
                .orElseThrow(() -> new RuntimeException("User not found: " + residentId));

        // GRASP Creator + Factory Method
        MaintenanceRequest req = requestFactory.createRequest(
                title, description, category, priority, resident);
        MaintenanceRequest saved = requestRepository.save(req);

        notificationService.notifyAll("REQUEST_RAISED",
                "New " + priority + " request: " + title, saved.getId());
        auditLogger.log("REQUEST_RAISED", "MaintenanceRequest",
                saved.getId(), residentId, "Priority: " + priority);

        return saved;
    }

    /** ASSIGN — Admin assigns a vendor/staff */
    @Transactional
    public MaintenanceRequest assignVendor(Long requestId, Long vendorId, Long adminId) {
        MaintenanceRequest req = findOrThrow(requestId);
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + vendorId));

        req.setAssignedTo(vendor);
        req.updateStatus(RequestStatus.ASSIGNED);
        MaintenanceRequest saved = requestRepository.save(req);

        notificationService.notifyAll("REQUEST_ASSIGNED",
                "Request #" + requestId + " assigned to " + vendor.getName(), requestId);
        auditLogger.log("REQUEST_ASSIGNED", "MaintenanceRequest",
                requestId, adminId, "Assigned to vendor ID: " + vendorId);

        return saved;
    }

    /** STATUS UPDATE — Vendor updates progress */
    @Transactional
    public MaintenanceRequest updateStatus(Long requestId, RequestStatus newStatus,
                                           Long actorId, String workSummary) {
        MaintenanceRequest req = findOrThrow(requestId);
        req.updateStatus(newStatus);
        if (workSummary != null && !workSummary.isBlank()) {
            req.setWorkSummary(workSummary);
        }
        MaintenanceRequest saved = requestRepository.save(req);

        notificationService.notifyAll("REQUEST_STATUS_CHANGED",
                "Request #" + requestId + " → " + newStatus, requestId);
        auditLogger.log("STATUS_UPDATED", "MaintenanceRequest",
                requestId, actorId, "New status: " + newStatus);

        return saved;
    }

    /** CLOSE — Admin verifies and closes */
    @Transactional
    public MaintenanceRequest closeRequest(Long requestId, Long adminId,
                                           String workSummary, double cost) {
        MaintenanceRequest req = findOrThrow(requestId);
        req.updateStatus(RequestStatus.CLOSED);
        req.setWorkSummary(workSummary);
        req.setCostIncurred(cost);
        MaintenanceRequest saved = requestRepository.save(req);

        notificationService.notifyAll("REQUEST_CLOSED",
                "Request #" + requestId + " closed. Cost: ₹" + cost, requestId);
        auditLogger.log("REQUEST_CLOSED", "MaintenanceRequest",
                requestId, adminId, "Cost: " + cost + ", Summary: " + workSummary);

        return saved;
    }

    /** REJECT */
    @Transactional
    public MaintenanceRequest rejectRequest(Long requestId, Long adminId, String reason) {
        MaintenanceRequest req = findOrThrow(requestId);
        req.updateStatus(RequestStatus.REJECTED);
        req.setWorkSummary("Rejected: " + reason);
        MaintenanceRequest saved = requestRepository.save(req);
        auditLogger.log("REQUEST_REJECTED", "MaintenanceRequest",
                requestId, adminId, reason);
        return saved;
    }

    /** FEEDBACK — Resident rates closed request */
    @Transactional
    public Feedback submitFeedback(Long requestId, Long residentId,
                                   int rating, String comment) {
        MaintenanceRequest req = findOrThrow(requestId);
        User resident = userRepository.findById(residentId)
                .orElseThrow(() -> new RuntimeException("User not found: " + residentId));

        Feedback fb = new Feedback();
        fb.setRequest(req);
        fb.setGivenBy(resident);
        fb.setRating(rating);
        fb.setComment(comment);
        Feedback saved = feedbackRepository.save(fb);

        // Update vendor rating average
        if (req.getAssignedTo() != null) {
            Double avg = feedbackRepository.getAverageRatingForVendor(req.getAssignedTo());
            if (avg != null) {
                User vendor = req.getAssignedTo();
                vendor.setRating(Math.round(avg * 10.0) / 10.0);
                userRepository.save(vendor);
            }
        }

        auditLogger.log("FEEDBACK_SUBMITTED", "Feedback",
                saved.getId(), residentId, "Rating: " + rating);
        return saved;
    }

    public List<MaintenanceRequest> getAllRequests() {
        return requestRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<MaintenanceRequest> getRequestsByResident(Long residentId) {
        User u = userRepository.findById(residentId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return requestRepository.findByRaisedBy(u);
    }

    public List<MaintenanceRequest> getRequestsByVendor(Long vendorId) {
        User v = userRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        return requestRepository.findByAssignedTo(v);
    }

    public Optional<MaintenanceRequest> getById(Long id) {
        return requestRepository.findById(id);
    }

    private MaintenanceRequest findOrThrow(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found: " + id));
    }
}
