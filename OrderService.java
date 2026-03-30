package com.ecommerce.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrderService {
	private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final ProductService productService;
    private final CartService cartService;
    private final FraudService fraudService;
 
    // Task 19: idempotency key -> orderId
    private final Map<String, String> idempotencyKeys = new ConcurrentHashMap<>();
 
    private int orderCounter = 100;
 
    public OrderService(ProductService productService, CartService cartService, FraudService fraudService,PaymentService paymentService) {
        this.productService = productService;
        this.cartService = cartService;
        this.fraudService = fraudService;
		 this.paymentService  = paymentService;
    }
 
    // Task 5: Place order — atomic
    public Order placeOrder(String userId, String couponCode) {
        // Task 19: idempotency — use userId as key for simplicity
        String idemKey = userId + "-" + System.currentTimeMillis() / 10000; // 10s window
        if (idempotencyKeys.containsKey(userId)) {
            String existingOrderId = idempotencyKeys.get(userId);
            Order existing = orders.get(existingOrderId);
            if (existing != null && existing.getState() == Order.State.PENDING_PAYMENT) {
                System.out.println("    Duplicate order detected (idempotency). Returning existing order: " + existingOrderId);
                return existing;
            }
        }
 
        if (cartService.isCartEmpty(userId)) {
            System.out.println("   Cart is empty.");
            return null;
        }
 
        Map<String, CartItem> cartItems = cartService.getCartItems(userId);
        double[] totals = cartService.calculateTotal(userId, couponCode);
        double subtotal = totals[0], discount = totals[1], total = totals[2];
 
        String orderId = "ORDER_" + (++orderCounter);
        List<CartItem> itemSnapshot = new ArrayList<>(cartItems.values());
 
        Order order = new Order(orderId, userId, itemSnapshot, total, discount);
        AuditLog.log(orderId + " created for user " + userId + " total=₹" + total);
 
        // Transition: CREATED -> PENDING_PAYMENT
        order.transitionTo(Order.State.PENDING_PAYMENT);
 
        // Task 6: Simulate payment
        boolean paymentSuccess = simulatePayment(orderId, total);
 
        if (!paymentSuccess) {
            // Task 7: Rollback — restore stock, mark failed
            System.out.println("   Payment failed. Rolling back...");
            for (CartItem item : itemSnapshot) {
                productService.releaseReservation(item.getProduct().getId(), item.getQuantity(), userId);
            }
            order.transitionTo(Order.State.FAILED);
            orders.put(orderId, order);
            AuditLog.log(orderId + " FAILED — stock reservation released (rollback)");
            return order;
        }
 
        // Payment success — deduct stock
        for (CartItem item : itemSnapshot) {
            productService.deductStock(item.getProduct().getId(), item.getQuantity(), userId);
        }
        order.transitionTo(Order.State.PAID);
        orders.put(orderId, order);
        cartService.clearCart(userId);
        idempotencyKeys.put(userId, orderId);
 
        // Task 17: Fraud check
        fraudService.checkFraud(userId, order, orders);
 
        AuditLog.log(orderId + " PAID — stock deducted, cart cleared");
        System.out.printf("   Order placed! ID: %s | Total: ₹%.2f (Saved: ₹%.2f)%n", orderId, total, discount);
        return order;
    }
 
    // Task 6: Payment simulation (random failure)
    private boolean simulatePayment(String orderId, double amount) {
        System.out.println("  💳 Processing payment for " + orderId + " (₹" + amount + ")...");
        boolean success = Math.random() > 0.25; // 75% success
        System.out.println(success ? "   Payment SUCCESS" : "   Payment FAILED");
        return success;
    }
 
    // Task 12: Cancel order
    public boolean cancelOrder(String orderId, String userId) {
        Order order = orders.get(orderId);
        if (order == null) { System.out.println("   Order not found."); return false; }
        if (!order.getUserId().equals(userId)) { System.out.println("   Unauthorized."); return false; }
 
        if (!order.transitionTo(Order.State.CANCELLED)) {
            System.out.println("  ❌ Cannot cancel order in state: " + order.getState());
            return false;
        }
        // Restore stock
        for (CartItem item : order.getItems()) {
            productService.restoreStock(item.getProduct().getId(), item.getQuantity());
        }
        AuditLog.log(orderId + " CANCELLED by " + userId + " — stock restored");
        System.out.println("   Order cancelled and stock restored.");
        return true;
    }
 
    // Task 13: Return & Refund
    public boolean returnProduct(String orderId, String userId, String productId, int qty) {
        Order order = orders.get(orderId);
        if (order == null) { System.out.println("   Order not found."); return false; }
        if (order.getState() != Order.State.DELIVERED && order.getState() != Order.State.PAID) {
            System.out.println("   Can only return delivered/paid orders."); return false;
        }
 
        CartItem foundItem = order.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst().orElse(null);
 
        if (foundItem == null) { System.out.println("   Product not in this order."); return false; }
        int alreadyReturned = order.getReturnedItems().stream()
                .filter(r -> r.productId.equals(productId))
                .mapToInt(r -> r.quantity).sum();
        int returnable = foundItem.getQuantity() - alreadyReturned;
        if (qty > returnable) {
            System.out.println("   Can only return up to " + returnable + " units.");
            return false;
        }
 
        double refund = foundItem.getProduct().getPrice() * qty;
        order.getReturnedItems().add(new Order.ReturnItem(productId, foundItem.getProduct().getName(), qty, refund));
        order.setTotalAmount(order.getTotalAmount() - refund);
        productService.restoreStock(productId, qty);
        AuditLog.log(orderId + ": returned " + qty + "x " + productId + " refund=₹" + refund);
        System.out.printf("   Return processed. Refund: ₹%.2f | New order total: ₹%.2f%n", refund, order.getTotalAmount());
        return true;
    }
 
    // Task 11: View all orders
    public void viewOrders(String filterStatus) {
        List<Order> list = orders.values().stream()
                .filter(o -> filterStatus == null || o.getState().name().equalsIgnoreCase(filterStatus))
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .collect(Collectors.toList());
 
        if (list.isEmpty()) { System.out.println("  No orders found."); return; }
        list.forEach(o -> System.out.println("  " + o));
    }
 
    // Task 11: Search by order ID
    public void searchOrder(String orderId) {
        Order o = orders.get(orderId);
        if (o == null) { System.out.println("   Order not found."); return; }
        System.out.println("  " + o);
        System.out.println("  Items:");
        o.getItems().forEach(i -> System.out.println("    " + i));
        if (!o.getReturnedItems().isEmpty()) {
            System.out.println("  Returns:");
            o.getReturnedItems().forEach(r -> System.out.printf("    %s x%d refund=₹%.2f%n", r.productName, r.quantity, r.refundAmount));
        }
    }
 
    // Task 8: Advance state (e.g., mark as shipped/delivered)
    public boolean advanceOrderState(String orderId, Order.State newState) {
        Order order = orders.get(orderId);
        if (order == null) { System.out.println("  Order not found."); return false; }
        if (order.transitionTo(newState)) {
            AuditLog.log(orderId + " transitioned to " + newState);
            System.out.println("   Order " + orderId + " is now: " + newState);
            return true;
        }
        System.out.println("   Invalid transition from " + order.getState() + " to " + newState);
        return false;
    }
 
    public Map<String, Order> getOrders() { return orders; }

}
