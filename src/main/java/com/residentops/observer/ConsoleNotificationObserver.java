package com.residentops.observer;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Concrete Observer: logs all events to console.
 * In production you'd add: EmailObserver, PushNotificationObserver, etc.
 * Each is a new class — no changes to NotificationService (OCP).
 */
@Component
public class ConsoleNotificationObserver implements IObserver {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void onEvent(String eventType, String message, Long entityId) {
        System.out.printf("[%s] EVENT: %-30s | Entity #%d | %s%n",
                LocalDateTime.now().format(FMT),
                eventType,
                entityId == null ? 0 : entityId,
                message);
    }
}
