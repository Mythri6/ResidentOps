package com.residentops.service;

import com.residentops.model.MaintenanceRequest;
import com.residentops.model.enums.RequestStatus;
import com.residentops.repository.MaintenanceRequestRepository;
import com.residentops.singleton.AuditLogger;
import com.residentops.singleton.NotificationService;
import com.residentops.strategy.IEscalationStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SLA Service — SOLID SRP
 *
 * Owns ONLY SLA-related responsibilities:
 *   1. Detecting breached requests
 *   2. Triggering escalation via Strategy
 *   3. Scheduled SLA scanner (runs every 5 minutes)
 *
 * SOLID — DIP: Depends on IEscalationStrategy interface, not concrete classes.
 * SOLID — OCP: Swap escalation channel by changing the @Qualifier injection.
 */
@Service
@EnableScheduling
public class SLAService {

    private final MaintenanceRequestRepository requestRepository;
    private final IEscalationStrategy escalationStrategy;
    private final NotificationService notificationService;
    private final AuditLogger auditLogger;

    // DIP — inject the interface; swap @Qualifier to change channel
    public SLAService(MaintenanceRequestRepository requestRepository,
                      @Qualifier("emailEscalation") IEscalationStrategy escalationStrategy,
                      NotificationService notificationService,
                      AuditLogger auditLogger) {
        this.requestRepository = requestRepository;
        this.escalationStrategy = escalationStrategy;
        this.notificationService = notificationService;
        this.auditLogger = auditLogger;
    }

    /**
     * Scheduled SLA scanner — runs every 5 minutes.
     * Finds all breached requests and escalates them automatically.
     */
    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void checkAndEscalate() {
        List<MaintenanceRequest> breached =
                requestRepository.findSLABreached(LocalDateTime.now());

        for (MaintenanceRequest req : breached) {
            escalationStrategy.escalate(req);
            req.updateStatus(RequestStatus.ESCALATED);
            requestRepository.save(req);

            notificationService.notifyAll(
                    "SLA_BREACHED",
                    "Request #" + req.getId() + " '" + req.getTitle() + "' SLA breached",
                    req.getId());

            // Audit — system user ID 0 means automated system action
            auditLogger.log("SLA_AUTO_ESCALATED", "MaintenanceRequest",
                    req.getId(), req.getRaisedBy().getId(),
                    "Auto-escalated via " + escalationStrategy.getChannelName());
        }

        if (!breached.isEmpty()) {
            System.out.println("[SLAService] Escalated " + breached.size() + " request(s).");
        }
    }

    /** Manual trigger for immediate SLA check (used by admin endpoint) */
    @Transactional
    public int runManualCheck() {
        List<MaintenanceRequest> breached =
                requestRepository.findSLABreached(LocalDateTime.now());
        breached.forEach(req -> {
            escalationStrategy.escalate(req);
            req.updateStatus(RequestStatus.ESCALATED);
            requestRepository.save(req);
        });
        return breached.size();
    }
}
