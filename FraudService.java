package com.ecommerce.model;

import java.time.LocalDateTime;
import java.util.Map;

public class FraudService {
	 private static final int MAX_ORDERS_PER_MINUTE = 3;
	    private static final double HIGH_VALUE_THRESHOLD = 10000.0;
	 
	    public void checkFraud(String userId, Order newOrder, Map<String, Order> allOrders) {
	        // Rule 1: count orders by user in last minute
	        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
	        long recentOrders = allOrders.values().stream()
	                .filter(o -> o.getUserId().equals(userId))
	                .filter(o -> o.getCreatedAt().isAfter(oneMinuteAgo))
	                .count();
	 
	        if (recentOrders >= MAX_ORDERS_PER_MINUTE) {
	            newOrder.setFraudFlagged(true);
	            AuditLog.log(" FRAUD: User " + userId + " placed " + recentOrders + " orders in 1 minute");
	            System.out.println("    FRAUD ALERT: Suspicious order frequency detected for " + userId);
	        }
	 
	        // Rule 2: High value
	        if (newOrder.getTotalAmount() > HIGH_VALUE_THRESHOLD) {
	            newOrder.setFraudFlagged(true);
	            AuditLog.log(" FRAUD: High-value order " + newOrder.getOrderId() + " = ₹" + newOrder.getTotalAmount());
	            System.out.println("    FRAUD ALERT: High-value order flagged: ₹" + newOrder.getTotalAmount());
	        }
	    }

}
