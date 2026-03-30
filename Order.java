package com.ecommerce.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Order {
	public enum State {
        CREATED, PENDING_PAYMENT, PAID, SHIPPED, DELIVERED, FAILED, CANCELLED
    }
 
    private static final Map<State, Set<State>> VALID_TRANSITIONS = new HashMap<>();
 
    static {
        VALID_TRANSITIONS.put(State.CREATED,          EnumSet.of(State.PENDING_PAYMENT, State.CANCELLED));
        VALID_TRANSITIONS.put(State.PENDING_PAYMENT,  EnumSet.of(State.PAID, State.FAILED, State.CANCELLED));
        VALID_TRANSITIONS.put(State.PAID,             EnumSet.of(State.SHIPPED, State.CANCELLED));
        VALID_TRANSITIONS.put(State.SHIPPED,          EnumSet.of(State.DELIVERED));
        VALID_TRANSITIONS.put(State.DELIVERED,        EnumSet.noneOf(State.class));
        VALID_TRANSITIONS.put(State.FAILED,           EnumSet.noneOf(State.class));
        VALID_TRANSITIONS.put(State.CANCELLED,        EnumSet.noneOf(State.class));
    }
 
    private String orderId;
    private String userId;
    private List<CartItem> items;
    private double totalAmount;
    private double discountAmount;
    private State state;
    private LocalDateTime createdAt;
    private List<ReturnItem> returnedItems = new ArrayList<>();
    private boolean fraudFlagged = false;
 
    public Order(String orderId, String userId, List<CartItem> items, double totalAmount, double discountAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.items = new ArrayList<>(items);
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.state = State.CREATED;
        this.createdAt = LocalDateTime.now();
    }
 
    public boolean transitionTo(State newState) {
        Set<State> allowed = VALID_TRANSITIONS.getOrDefault(state, EnumSet.noneOf(State.class));
        if (allowed.contains(newState)) {
            this.state = newState;
            return true;
        }
        return false;
    }
 
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public List<CartItem> getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public double getDiscountAmount() { return discountAmount; }
    public State getState() { return state; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<ReturnItem> getReturnedItems() { return returnedItems; }
    public boolean isFraudFlagged() { return fraudFlagged; }
    public void setFraudFlagged(boolean v) { this.fraudFlagged = v; }
 
    @Override
    public String toString() {
        return String.format("[%s] User: %s | Total: ₹%.2f (Discount: ₹%.2f) | Status: %s%s | Items: %d | Created: %s",
                orderId, userId, totalAmount, discountAmount, state,
                fraudFlagged ? " FRAUD" : "", items.size(), createdAt.toLocalTime());
    }
 
    public static class ReturnItem {
        public String productId;
        public String productName;
        public int quantity;
        public double refundAmount;
 
        public ReturnItem(String productId, String productName, int quantity, double refundAmount) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.refundAmount = refundAmount;
        }
    }
}
