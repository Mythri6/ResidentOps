package com.residentops.observer;

/**
 * Observer Pattern (Behavioural) — IObserver
 *
 * Any component that wants to react to system events implements this.
 * SOLID — DIP: NotificationService depends on this abstraction.
 * SOLID — OCP: New event listeners added without changing the publisher.
 */
public interface IObserver {
    void onEvent(String eventType, String message, Long entityId);
}
