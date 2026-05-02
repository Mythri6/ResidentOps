package com.residentops.singleton;

import com.residentops.model.AuditLog;
import com.residentops.model.User;
import com.residentops.repository.AuditLogRepository;
import com.residentops.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Singleton Pattern — AuditLogger
 *
 * WHY SINGLETON: There must be exactly ONE audit log writer.
 * Multiple instances could write concurrently and create gaps.
 * Spring @Service guarantees single instance across the application.
 *
 * SOLID — SRP: Only responsible for writing and reading audit logs.
 *              Has no business logic, no domain decisions.
 */
@Service
public class AuditLogger {

    private static AuditLogger INSTANCE;

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogger(AuditLogRepository auditLogRepository,
                       UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        INSTANCE = this;
    }

    /**
     * Log an action to the persistent audit trail.
     *
     * @param action      e.g. "REQUEST_CLOSED", "EXPENSE_APPROVED"
     * @param entityType  e.g. "MaintenanceRequest", "Expense"
     * @param entityId    primary key of the affected entity
     * @param actorId     ID of the user who performed the action
     * @param details     free-text context
     */
    public void log(String action, String entityType, Long entityId,
                    Long actorId, String details) {
        userRepository.findById(actorId).ifPresent(actor -> {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setPerformedBy(actor);
            entry.setDetails(details);
            auditLogRepository.save(entry);
        });
    }

    public List<AuditLog> getAll() {
        return auditLogRepository.findAllByOrderByPerformedAtDesc();
    }

    public List<AuditLog> getByEntityType(String entityType) {
        return auditLogRepository.findByEntityType(entityType);
    }

    public List<AuditLog> getByActor(Long userId) {
        return auditLogRepository.findByPerformedById(userId);
    }

    public static AuditLogger getInstance() {
        return INSTANCE;
    }
}
