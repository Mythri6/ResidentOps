package com.residentops.strategy;

import com.residentops.model.MaintenanceRequest;
import org.springframework.stereotype.Component;

/**
 * Concrete Strategy: Email-based SLA escalation.
 * In production this would send real emails (JavaMailSender).
 * For the project demo it logs to console — easily swapped out.
 */
@Component("emailEscalation")
public class EmailEscalationStrategy implements IEscalationStrategy {

    @Override
    public void escalate(MaintenanceRequest request) {
        System.out.println("=== [EMAIL ESCALATION] ===");
        System.out.printf("  Request #%d: '%s'%n", request.getId(), request.getTitle());
        System.out.printf("  Priority  : %s%n", request.getPriority());
        System.out.printf("  SLA was   : %s%n", request.getSlaDeadline());
        System.out.println("  Email sent to committee@residentops.com");
        System.out.println("=========================");
    }

    @Override
    public String getChannelName() {
        return "EMAIL";
    }
}
