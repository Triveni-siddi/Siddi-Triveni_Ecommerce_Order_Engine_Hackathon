package com.ecommerce.model;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ProductService {
	private static final int LOW_STOCK_THRESHOLD = 3;
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> productLocks = new ConcurrentHashMap<>();
 
    // Task 15: reservation expiry tracking {productId -> {userId -> expiryTime}}
    private final Map<String, Map<String, Long>> reservationExpiry = new ConcurrentHashMap<>();
 
    private ReentrantLock getLock(String productId) {
        return productLocks.computeIfAbsent(productId, id -> new ReentrantLock());
    }
 
    // Task 1: Add product
    public boolean addProduct(String id, String name, double price, int stock) {
        if (products.containsKey(id)) {
            System.out.println("   Product ID already exists: " + id);
            return false;
        }
        if (stock < 0) { System.out.println("   Stock cannot be negative."); return false; }
        products.put(id, new Product(id, name, price, stock));
        AuditLog.log("Product " + id + " (" + name + ") added with stock=" + stock);
        return true;
    }
 
    public void viewProducts() {
        if (products.isEmpty()) { System.out.println("  No products available."); return; }
        products.values().forEach(p -> System.out.println("  " + p));
    }
 
    public Product getProduct(String id) { return products.get(id); }
    public Map<String, Product> getAllProducts() { return products; }
 
    // Task 3: Reserve stock when added to cart (with lock — Task 4)
    public boolean reserveStock(String productId, int qty, String userId) {
        ReentrantLock lock = getLock(productId);
        lock.lock();
        try {
            Product p = products.get(productId);
            if (p == null) { System.out.println("   Product not found."); return false; }
            if (p.reserveStock(qty)) {
                // Task 15: set expiry 5 minutes from now
                reservationExpiry.computeIfAbsent(productId, k -> new ConcurrentHashMap<>())
                        .put(userId, System.currentTimeMillis() + 5 * 60 * 1000);
                AuditLog.log("Stock reserved: " + qty + " of " + productId + " for user " + userId);
                return true;
            } else {
                System.out.printf("   Insufficient stock. Available: %d%n", p.getAvailableStock());
                return false;
            }
        } finally {
            lock.unlock();
        }
    }
 
    public void releaseReservation(String productId, int qty, String userId) {
        ReentrantLock lock = getLock(productId);
        lock.lock();
        try {
            Product p = products.get(productId);
            if (p != null) {
                p.releaseReservation(qty);
                Map<String, Long> expMap = reservationExpiry.get(productId);
                if (expMap != null) expMap.remove(userId);
                AuditLog.log("Reservation released: " + qty + " of " + productId + " for user " + userId);
            }
        } finally {
            lock.unlock();
        }
    }
 
    public void deductStock(String productId, int qty, String userId) {
        ReentrantLock lock = getLock(productId);
        lock.lock();
        try {
            Product p = products.get(productId);
            if (p != null) {
                p.deductStock(qty);
                Map<String, Long> expMap = reservationExpiry.get(productId);
                if (expMap != null) expMap.remove(userId);
            }
        } finally {
            lock.unlock();
        }
    }
 
    public void restoreStock(String productId, int qty) {
        Product p = products.get(productId);
        if (p != null) {
            p.restoreStock(qty);
            AuditLog.log("Stock restored: " + qty + " of " + productId);
        }
    }
 
    // Task 10: Low stock alert
    public void checkLowStock() {
        System.out.println("  === Low Stock Alert ===");
        boolean found = false;
        for (Product p : products.values()) {
            if (p.getAvailableStock() <= LOW_STOCK_THRESHOLD) {
                System.out.printf("  ⚠️  [%s] %s — Available: %d%n", p.getId(), p.getName(), p.getAvailableStock());
                found = true;
            }
        }
        if (!found) System.out.println("  ✅ All products have sufficient stock.");
    }
 
    // Task 15: expire old reservations
    public void expireReservations() {
        long now = System.currentTimeMillis();
        System.out.println("  === Checking Reservation Expiry ===");
        int count = 0;
        for (Map.Entry<String, Map<String, Long>> prodEntry : reservationExpiry.entrySet()) {
            String productId = prodEntry.getKey();
            Iterator<Map.Entry<String, Long>> it = prodEntry.getValue().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> e = it.next();
                if (now > e.getValue()) {
                    Product p = products.get(productId);
                    if (p != null) p.releaseReservation(1); // release 1 per expired entry
                    AuditLog.log("Reservation expired for user " + e.getKey() + " on product " + productId);
                    it.remove();
                    count++;
                }
            }
        }
        System.out.println("  Expired reservations released: " + count);
    }

}
