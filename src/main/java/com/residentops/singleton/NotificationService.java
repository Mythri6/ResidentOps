package com.residentops.singleton;

import com.residentops.observer.IObserver;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton Pattern + Observer Publisher
 *
 * WHY SINGLETON: There must be exactly ONE notification dispatcher in
 * the system so all events flow through a single, auditable channel.
 * Spring @Service is a singleton by default — this enforces the pattern
 * architecturally AND documents the intent explicitly.
 *
 * Maintains a list of IObserver subscribers and fans out every event.
 * SOLID — OCP: New subscribers added via register() without touching this class.
 * SOLID — DIP: Depends on IObserver abstraction, not concrete classes.
 */
@Service
public class NotificationService {

    // Spring creates exactly ONE instance — Singleton guaranteed
    private static NotificationService INSTANCE;

    private final List<IObserver> observers = new ArrayList<>();

    // Spring injects all IObserver beans automatically
    public NotificationService(List<IObserver> observers) {
        this.observers.addAll(observers);
        INSTANCE = this;
    }

    /** Register additional observers at runtime */
    public void register(IObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /** Remove an observer */
    public void unregister(IObserver observer) {
        observers.remove(observer);
    }

    /**
     * Publish an event to all registered observers.
     * @param eventType  e.g. "REQUEST_RAISED", "SLA_BREACHED", "POLL_CLOSED"
     * @param message    human-readable description
     * @param entityId   ID of the related entity (may be null)
     */
    public void notifyAll(String eventType, String message, Long entityId) {
        for (IObserver obs : observers) {
            try {
                obs.onEvent(eventType, message, entityId);
            } catch (Exception e) {
                System.err.println("Observer error: " + e.getMessage());
            }
        }
    }

    /** Convenience: broadcast without entity ID */
    public void notifyAll(String eventType, String message) {
        notifyAll(eventType, message, null);
    }

    // Expose for non-Spring contexts (satisfies Singleton documentation)
    public static NotificationService getInstance() {
        return INSTANCE;
    }
}
