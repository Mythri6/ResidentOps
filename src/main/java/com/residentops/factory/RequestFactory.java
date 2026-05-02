package com.residentops.factory;

import com.residentops.model.MaintenanceRequest;
import com.residentops.model.User;
import com.residentops.model.enums.Priority;
import com.residentops.model.enums.RequestCategory;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Factory / Creator Pattern (Creational)
 *
 * WHY FACTORY: Callers (Controller, Service) should not need to know
 * how to construct a MaintenanceRequest with the correct SLA deadline.
 * That knowledge lives here — one place, one change.
 *
 * GRASP — Creator: RequestFactory has the initializing data (priority → SLA rules)
 *                  so it is the natural creator of MaintenanceRequest objects.
 *
 * SOLID — SRP: Object creation is isolated from business logic.
 * SOLID — OCP: To add a new priority level, extend the SLA map — no changes
 *              to any service or controller.
 */
@Component
public class RequestFactory {

    /**
     * SLA rules table — maps Priority → hours until deadline.
     * Easy to externalise to DB or config in production.
     */
    private static final Map<Priority, Integer> SLA_HOURS = Map.of(
            Priority.CRITICAL, 2,
            Priority.HIGH,     4,
            Priority.MEDIUM,   24,
            Priority.LOW,      48
    );

    /**
     * Create a fully-initialised MaintenanceRequest with SLA deadline computed.
     *
     * @param title       short title from resident
     * @param description detailed description
     * @param category    plumbing / electrical / etc.
     * @param priority    determines SLA window
     * @param raisedBy    resident who raised the request
     * @return            request ready to be persisted
     */
    public MaintenanceRequest createRequest(String title,
                                            String description,
                                            RequestCategory category,
                                            Priority priority,
                                            User raisedBy) {
        MaintenanceRequest request = new MaintenanceRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setCategory(category);
        request.setPriority(priority);
        request.setRaisedBy(raisedBy);
        request.setSlaDeadline(computeDeadline(priority));
        return request;
    }

    /** Compute SLA deadline from current time + priority-based hours */
    private LocalDateTime computeDeadline(Priority priority) {
        int hours = SLA_HOURS.getOrDefault(priority, 24);
        return LocalDateTime.now().plusHours(hours);
    }

    /** Expose SLA hours for display/info purposes */
    public int getSlaHours(Priority priority) {
        return SLA_HOURS.getOrDefault(priority, 24);
    }
}
