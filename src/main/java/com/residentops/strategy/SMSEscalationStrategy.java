package com.residentops.strategy;

import com.residentops.model.MaintenanceRequest;
import org.springframework.stereotype.Component;

/**
 * Concrete Strategy: SMS-based SLA escalation.
 * Demonstrates OCP — added without touching SLAService.
 */
@Component("smsEscalation")
public class SMSEscalationStrategy implements IEscalationStrategy {

    @Override
    public void escalate(MaintenanceRequest request) {
        System.out.println("=== [SMS ESCALATION] ===");
        System.out.printf("  Request #%d SLA breached!%n", request.getId());
        System.out.printf("  SMS sent to committee mobile: +91-XXXXXXXXXX%n");
        System.out.println("========================");
    }

    @Override
    public String getChannelName() {
        return "SMS";
    }
}
