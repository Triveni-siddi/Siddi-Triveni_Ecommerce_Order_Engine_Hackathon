package com.ecommerce.model;

import java.util.LinkedList;
import java.util.Queue;

public class EventService {
	public enum EventType {
        ORDER_CREATED, PAYMENT_SUCCESS, INVENTORY_UPDATED
    }
 
    private final Queue<EventType> eventQueue = new LinkedList<>();
 
    public void enqueue(EventType... events) {
        for (EventType e : events) {
            eventQueue.add(e);
            System.out.println("  [EVENT QUEUED] " + e);
        }
    }
 
    public void processAll() {
        System.out.println("\n  === Processing Event Queue ===");
        while (!eventQueue.isEmpty()) {
            EventType event = eventQueue.poll();
            boolean success = process(event);
            AuditLog.log("Event " + event + " -> " + (success ? "SUCCESS" : "FAILED"));
            if (!success) {
                System.out.println("   Event " + event + " failed. Stopping queue. Remaining events dropped.");
                eventQueue.clear();
                return;
            }
        }
        System.out.println("   All events processed successfully.");
    }
 
    private boolean process(EventType event) {
        System.out.print("  Processing " + event + "... ");
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        // Simulate occasional failure (20% chance)
        boolean success = Math.random() > 0.2;
        System.out.println(success ? "Yes" : "No");
        return success;
    }
 
    public void simulateFullOrderFlow() {
        enqueue(EventType.ORDER_CREATED, EventType.PAYMENT_SUCCESS, EventType.INVENTORY_UPDATED);
        processAll();
    }

}
