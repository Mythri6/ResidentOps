package com.residentops.strategy;

import com.residentops.model.MaintenanceRequest;

/**
 * Strategy Pattern (Behavioural) — IEscalationStrategy
 *
 * SOLID — OCP: New escalation channels (WhatsApp, Push) can be added
 *              by implementing this interface without modifying SLAService.
 * SOLID — DIP: SLAService depends on this abstraction, not on
 *              concrete EmailEscalationStrategy or SMSEscalationStrategy.
 */
public interface IEscalationStrategy {
    void escalate(MaintenanceRequest request);
    String getChannelName();
}
