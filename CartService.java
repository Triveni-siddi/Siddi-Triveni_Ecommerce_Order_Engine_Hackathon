package com.ecommerce.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CartService {
	private final Map<String, Map<String, CartItem>> userCarts = new HashMap<>();
    private final ProductService productService;
 
    // Task 9: coupon codes
    private static final Map<String, Double> COUPON_FLAT = new HashMap<>();
    private static final Map<String, Double> COUPON_PCT  = new HashMap<>();
 
    static {
        COUPON_FLAT.put("FLAT200", 200.0);
        COUPON_PCT.put("SAVE10", 0.10);
    }
 
    public CartService(ProductService productService) {
        this.productService = productService;
    }
 
    private Map<String, CartItem> getCart(String userId) {
        return userCarts.computeIfAbsent(userId, u -> new LinkedHashMap<>());
    }
 
    public boolean addToCart(String userId, String productId, int qty) {
        Product p = productService.getProduct(productId);
        if (p == null) { System.out.println("  ❌ Product not found."); return false; }
        if (qty <= 0)  { System.out.println("  ❌ Quantity must be positive."); return false; }
 
        Map<String, CartItem> cart = getCart(userId);
        int alreadyInCart = cart.containsKey(productId) ? cart.get(productId).getQuantity() : 0;
 
        if (!productService.reserveStock(productId, qty, userId)) return false;
 
        if (cart.containsKey(productId)) {
            cart.get(productId).setQuantity(alreadyInCart + qty);
        } else {
            cart.put(productId, new CartItem(p, qty));
        }
        AuditLog.log(userId + " added " + productId + " qty=" + qty + " to cart");
        return true;
    }
 
    public boolean removeFromCart(String userId, String productId, int qty) {
        Map<String, CartItem> cart = getCart(userId);
        if (!cart.containsKey(productId)) { System.out.println("  ❌ Item not in cart."); return false; }
        CartItem item = cart.get(productId);
        int newQty = item.getQuantity() - qty;
        if (newQty < 0) { System.out.println("  ❌ Cannot remove more than in cart."); return false; }
 
        productService.releaseReservation(productId, qty, userId);
        if (newQty == 0) cart.remove(productId);
        else item.setQuantity(newQty);
        AuditLog.log(userId + " removed " + productId + " qty=" + qty + " from cart");
        return true;
    }
 
    public void viewCart(String userId) {
        Map<String, CartItem> cart = getCart(userId);
        if (cart.isEmpty()) { System.out.println("  Cart is empty."); return; }
        System.out.println("  === Cart for " + userId + " ===");
        double total = 0;
        for (CartItem item : cart.values()) {
            System.out.println("  " + item);
            total += item.getSubtotal();
        }
        System.out.printf("  Subtotal: ₹%.2f%n", total);
    }
 
    // Task 9: Calculate total with discounts
    public double[] calculateTotal(String userId, String couponCode) {
        Map<String, CartItem> cart = getCart(userId);
        double subtotal = cart.values().stream().mapToDouble(CartItem::getSubtotal).sum();
        double discount = 0;
 
        // Auto discount rules
        if (subtotal > 1000) discount += subtotal * 0.10;          // 10% on total > 1000
        for (CartItem item : cart.values()) {
            if (item.getQuantity() > 3) discount += item.getSubtotal() * 0.05; // extra 5% if qty > 3
        }
 
        // Coupon code
        if (couponCode != null && !couponCode.isEmpty()) {
            String upper = couponCode.toUpperCase();
            if (COUPON_FLAT.containsKey(upper)) {
                discount += COUPON_FLAT.get(upper);
                System.out.println("  ✅ Coupon applied: -₹" + COUPON_FLAT.get(upper));
            } else if (COUPON_PCT.containsKey(upper)) {
                double pctDiscount = subtotal * COUPON_PCT.get(upper);
                discount += pctDiscount;
                System.out.printf("  ✅ Coupon applied: -₹%.2f%n", pctDiscount);
            } else {
                System.out.println("  ❌ Invalid coupon code.");
            }
        }
 
        double total = Math.max(0, subtotal - discount);
        return new double[]{subtotal, discount, total};
    }
 
    public Map<String, CartItem> getCartItems(String userId) {
        return getCart(userId);
    }
 
    public void clearCart(String userId) {
        userCarts.put(userId, new LinkedHashMap<>());
    }
 
    public boolean isCartEmpty(String userId) {
        return getCart(userId).isEmpty();
    }
}
