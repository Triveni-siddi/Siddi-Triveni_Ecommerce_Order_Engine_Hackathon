package com.ecommerce.model;

import java.util.Scanner;

public class Main {
	static ProductService     productService;
    static CartService        cartService;
    static FraudService       fraudService;
    static OrderService       orderService;
    static EventService       eventService;
    static ConcurrencyService concurrencyService;
    static Scanner sc = new Scanner(System.in);
 
    public static void main(String[] args) {
        // Wire up services (Task 20: microservice modules)
        productService     = new ProductService();
        cartService        = new CartService(productService);
        fraudService       = new FraudService();
        orderService       = new OrderService(productService, cartService, fraudService);
        eventService       = new EventService();
        concurrencyService = new ConcurrencyService(productService, cartService);
 
        seedData();
        printBanner();
 
        while (true) {
            printMenu();
            System.out.print("Enter choice: ");
            String choice = sc.nextLine().trim();
 
            switch (choice) {
                case "1"  -> addProduct();
                case "2"  -> productService.viewProducts();
                case "3"  -> addToCart();
                case "4"  -> removeFromCart();
                case "5"  -> viewCart();
                case "6"  -> applyCouponPreview();
                case "7"  -> placeOrder();
                case "8"  -> cancelOrder();
                case "9"  -> viewOrders();
                case "10" -> productService.checkLowStock();
                case "11" -> returnProduct();
                case "12" -> simulateConcurrentUsers();
                case "13" -> AuditLog.printAll();
                case "14" -> concurrencyService.triggerFailureMode();
                case "15" -> eventService.simulateFullOrderFlow();
                case "16" -> productService.expireReservations();
                case "17" -> concurrencyService.showMicroserviceStatus();
                case "18" -> searchOrder();
                case "19" -> advanceOrderState();
                case "0"  -> { System.out.println("\n  Goodbye! 👋"); return; }
                default   -> System.out.println("  ❌ Invalid option.");
            }
            System.out.println();
        }
    }
 
    static void printBanner() {
        System.out.println("\n═════════════════════════════════════════════════");
        System.out.println("    Distributed E-Commerce Order Engine v1.0   ");
        System.out.println("══════════════════════════════════════════════════");
        System.out.println("  Sample data seeded. Ready!\n");
    }
 
    static void printMenu() {
        System.out.println("─────────────────────────────────────────");
        System.out.println(" 1.  Add Product         2.  View Products");
        System.out.println(" 3.  Add to Cart         4.  Remove from Cart");
        System.out.println(" 5.  View Cart           6.  Apply Coupon (preview)");
        System.out.println(" 7.  Place Order         8.  Cancel Order");
        System.out.println(" 9.  View Orders        10.  Low Stock Alert");
        System.out.println("11.  Return Product     12.  Simulate Concurrent Users");
        System.out.println("13.  View Logs          14.  Trigger Failure Mode");
        System.out.println("15.  Run Event Queue    16.  Expire Reservations");
        System.out.println("17.  Microservice Status 18. Search Order");
        System.out.println("19.  Advance Order State");
        System.out.println(" 0.  Exit");
        System.out.println("─────────────────────────────────────────");
    }
 
 
    static String prompt(String msg) {
        System.out.print("  " + msg + ": ");
        return sc.nextLine().trim();
    }
 
    static int promptInt(String msg) {
        try { return Integer.parseInt(prompt(msg)); }
        catch (NumberFormatException e) { System.out.println("  ❌ Invalid number."); return -1; }
    }
 
    static double promptDouble(String msg) {
        try { return Double.parseDouble(prompt(msg)); }
        catch (NumberFormatException e) { System.out.println("  ❌ Invalid number."); return -1; }
    }
 
    // ── Menu handlers ──────────────────────────────────────────────────────
 
    static void addProduct() {
        System.out.println("\n  === Add Product ===");
        String id    = prompt("Product ID");
        String name  = prompt("Name");
        double price = promptDouble("Price (₹)");
        int stock    = promptInt("Stock quantity");
        if (price < 0 || stock < 0) return;
        productService.addProduct(id, name, price, stock);
    }
 
    static void addToCart() {
        System.out.println("\n  === Add to Cart ===");
        String userId    = prompt("User ID");
        String productId = prompt("Product ID");
        int qty          = promptInt("Quantity");
        if (qty <= 0) return;
        cartService.addToCart(userId, productId, qty);
    }
 
    static void removeFromCart() {
        System.out.println("\n  === Remove from Cart ===");
        String userId    = prompt("User ID");
        String productId = prompt("Product ID");
        int qty          = promptInt("Quantity to remove");
        if (qty <= 0) return;
        cartService.removeFromCart(userId, productId, qty);
    }
 
    static void viewCart() {
        String userId = prompt("User ID");
        cartService.viewCart(userId);
    }
 
    static void applyCouponPreview() {
        System.out.println("\n  === Coupon Preview ===");
        System.out.println("  Available codes: SAVE10 (10% off), FLAT200 (₹200 off)");
        String userId = prompt("User ID");
        String coupon = prompt("Coupon code (or blank)");
        double[] totals = cartService.calculateTotal(userId, coupon);
        System.out.printf("  Subtotal: ₹%.2f | Discount: ₹%.2f | Total: ₹%.2f%n",
                totals[0], totals[1], totals[2]);
    }
 
    static void placeOrder() {
        System.out.println("\n  === Place Order ===");
        String userId = prompt("User ID");
        String coupon = prompt("Coupon code (or blank)");
        orderService.placeOrder(userId, coupon);
    }
 
    static void cancelOrder() {
        System.out.println("\n  === Cancel Order ===");
        String orderId = prompt("Order ID");
        String userId  = prompt("User ID");
        orderService.cancelOrder(orderId, userId);
    }
 
    static void viewOrders() {
        System.out.println("\n  === View Orders ===");
        System.out.println("  Filter by status (CREATED/PAID/SHIPPED/DELIVERED/FAILED/CANCELLED) or blank for all:");
        String filter = prompt("Status filter");
        orderService.viewOrders(filter.isEmpty() ? null : filter);
    }
 
    static void returnProduct() {
        System.out.println("\n  === Return Product ===");
        String orderId   = prompt("Order ID");
        String userId    = prompt("User ID");
        String productId = prompt("Product ID to return");
        int qty          = promptInt("Quantity");
        if (qty <= 0) return;
        orderService.returnProduct(orderId, userId, productId, qty);
    }
 
    static void simulateConcurrentUsers() {
        System.out.println("\n  === Concurrent Users Simulation ===");
        String productId = prompt("Product ID");
        int numUsers     = promptInt("Number of concurrent users");
        int qtyEach      = promptInt("Quantity each user wants");
        if (numUsers <= 0 || qtyEach <= 0) return;
        concurrencyService.simulateConcurrentUsers(productId, numUsers, qtyEach);
    }
 
    static void searchOrder() {
        String orderId = prompt("Order ID");
        orderService.searchOrder(orderId);
    }
	PaymentService paymentService = new PaymentService();
orderService = new OrderService(productService, cartService, fraudService, paymentService);
 
    static void advanceOrderState() {
        System.out.println("\n  === Advance Order State ===");
        String orderId = prompt("Order ID");
        System.out.println("  States: SHIPPED, DELIVERED, CANCELLED");
        String stateStr = prompt("New state");
        try {
            Order.State newState = Order.State.valueOf(stateStr.toUpperCase());
            orderService.advanceOrderState(orderId, newState);
        } catch (IllegalArgumentException e) {
            System.out.println("   Invalid state.");
        }
    }
 
    // ── Sample Data ────────────────────────────────────────────────────────
 
    static void seedData() {
        productService.addProduct("P001", "iPhone 15",       79999, 10);
        productService.addProduct("P002", "Samsung Galaxy",  49999, 5);
        productService.addProduct("P003", "Wireless Earbuds", 2999, 2);
        productService.addProduct("P004", "USB-C Cable",      499,  50);
        productService.addProduct("P005", "Laptop Stand",    1999,  3);
    }

}
