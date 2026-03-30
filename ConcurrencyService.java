package com.ecommerce.model;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyService {
	private final ProductService productService;
	private final CartService cartService;

	// Task 18: failure injection flags
	private static volatile boolean paymentFailureMode = false;
	private static volatile boolean orderCreationFailureMode = false;
	private static volatile boolean inventoryUpdateFailureMode = false;

	public ConcurrencyService(ProductService productService, CartService cartService) {
	    this.productService = productService;
	    this.cartService = cartService;
	}

	// Task 4: Simulate concurrent users trying to add same product
	public void simulateConcurrentUsers(String productId, int numUsers, int qtyEach) {
	    Product p = productService.getProduct(productId);
	    if (p == null) { System.out.println("  ❌ Product not found."); return; }

	    System.out.printf("%n  === Concurrency Simulation ===%n");
	    System.out.printf("  Product: %s | Stock: %d | Users: %d | Each wants: %d%n",
	            p.getName(), p.getAvailableStock(), numUsers, qtyEach);

	    ExecutorService executor = Executors.newFixedThreadPool(numUsers);
	    AtomicInteger success = new AtomicInteger(0);
	    AtomicInteger failed  = new AtomicInteger(0);
	    CountDownLatch latch = new CountDownLatch(1);
	    CountDownLatch done  = new CountDownLatch(numUsers);

	    for (int i = 1; i <= numUsers; i++) {
	        final String userId = "USER_" + i;
	        executor.submit(() -> {
	            try {
	                latch.await(); // all start simultaneously
	                boolean ok = productService.reserveStock(productId, qtyEach, userId);
	                if (ok) { success.incrementAndGet(); System.out.println("  ✅ " + userId + " reserved " + qtyEach); }
	                else    { failed.incrementAndGet();  System.out.println("  ❌ " + userId + " failed to reserve"); }
	            } catch (InterruptedException e) {
	                Thread.currentThread().interrupt();
	            } finally {
	                done.countDown();
	            }
	        });
	    }

	    latch.countDown(); // release all at once
	    try { done.await(10, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
	    executor.shutdown();

	    System.out.printf("  Result: %d succeeded, %d failed (Stock available: %d)%n",
	            success.get(), failed.get(), p.getAvailableStock());
	    AuditLog.log("Concurrency simulation: " + success.get() + " succeeded, " + failed.get() + " failed on " + productId);
	}

	// Task 18: Failure injection
	public void triggerFailureMode() {
	    System.out.println("  === Failure Injection Mode ===");
	    System.out.println("  Select failure to inject:");
	    System.out.println("  1. Payment failures");
	    System.out.println("  2. Order creation failures");
	    System.out.println("  3. Inventory update failures");
	    System.out.println("  4. Reset all failures");

	    java.util.Scanner sc = new java.util.Scanner(System.in);
	    System.out.print("  > ");
	    String choice = sc.nextLine().trim();

	    switch (choice) {
	        case "1" -> { paymentFailureMode = !paymentFailureMode;
	            System.out.println("  Payment failure mode: " + (paymentFailureMode ? "ON" : "OFF")); }
	        case "2" -> { orderCreationFailureMode = !orderCreationFailureMode;
	            System.out.println("  Order creation failure mode: " + (orderCreationFailureMode ? "ON" : "OFF")); }
	        case "3" -> { inventoryUpdateFailureMode = !inventoryUpdateFailureMode;
	            System.out.println("  Inventory update failure mode: " + (inventoryUpdateFailureMode ? "ON" : "OFF")); }
	        case "4" -> { paymentFailureMode = false; orderCreationFailureMode = false; inventoryUpdateFailureMode = false;
	            System.out.println("  All failure modes reset."); }
	        default -> System.out.println("  Invalid choice.");
	    }
	    AuditLog.log("Failure mode toggled: payment=" + paymentFailureMode +
	            " order=" + orderCreationFailureMode + " inventory=" + inventoryUpdateFailureMode);
	}

	// Task 20: Microservice simulation — print module status
	public void showMicroserviceStatus() {
	    System.out.println("\n  === Microservice Status ===");
	    printService("ProductService",  !inventoryUpdateFailureMode);
	    printService("CartService",     true);
	    printService("OrderService",    !orderCreationFailureMode);
	    printService("PaymentService",  !paymentFailureMode);
	    printService("FraudService",    true);
	    printService("EventService",    true);
	}

	private void printService(String name, boolean healthy) {
	    System.out.printf("  %-20s %s%n", name, healthy ? " HEALTHY" : " DEGRADED");
	}

	public static boolean isPaymentFailureMode() { return paymentFailureMode; }
	public static boolean isOrderCreationFailureMode() { return orderCreationFailureMode; }
	public static boolean isInventoryUpdateFailureMode() { return inventoryUpdateFailureMode; }
	
}
