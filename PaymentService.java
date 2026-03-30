package com.ecommerce.model;


/**
 * Task 6:  Payment Simulation
 * Task 7:  Transaction Rollback
 * Task 18: Failure Injection (payment failure mode)
 * Task 20: Microservice Simulation — PaymentService module
 */
public class PaymentService {

    private static final double SUCCESS_RATE = 0.75; // 75% success

    /**
     * Processes payment for an order.
     * If failure injection mode is ON, always fails.
     * Otherwise, randomly succeeds based on SUCCESS_RATE.
     */
    public PaymentResult processPayment(String orderId, String userId, double amount) {
        AuditLog.log("Payment initiated: " + orderId + " | User: " + userId + " | Amount: ₹" + amount);
        System.out.println("  💳 Processing payment...");
        System.out.println("  Order   : " + orderId);
        System.out.println("  User    : " + userId);
        System.out.printf ("  Amount  : ₹%.2f%n", amount);

        // Task 18: respect failure injection mode
        if (ConcurrencyService.isPaymentFailureMode()) {
            System.out.println("  ⚡ [Failure Injection] Payment forcefully failed.");
            AuditLog.log("Payment FAILED (injected failure): " + orderId);
            return new PaymentResult(false, "FAILURE_INJECTED", orderId);
        }

        // Simulate network/processing delay
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        boolean success = Math.random() < SUCCESS_RATE;
        String txnId = success ? "TXN-" + System.currentTimeMillis() : null;

        if (success) {
            System.out.println("  ✅ Payment SUCCESS | Transaction ID: " + txnId);
            AuditLog.log("Payment SUCCESS: " + orderId + " txnId=" + txnId + " amount=₹" + amount);
            return new PaymentResult(true, txnId, orderId);
        } else {
            System.out.println("  ❌ Payment FAILED | Reason: Gateway timeout");
            AuditLog.log("Payment FAILED: " + orderId + " reason=Gateway timeout");
            return new PaymentResult(false, "GATEWAY_TIMEOUT", orderId);
        }
    }

    /**
     * Refund simulation for cancelled or returned orders.
     */
    public void processRefund(String orderId, double amount) {
        System.out.printf("  💰 Refund initiated for %s — ₹%.2f%n", orderId, amount);
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        System.out.printf("  ✅ Refund processed successfully for %s — ₹%.2f%n", orderId, amount);
        AuditLog.log("Refund processed: " + orderId + " amount=₹" + amount);
    }

    // ── Result wrapper ────────────────────────────────────────────────────

    public static class PaymentResult {
        private final boolean success;
        private final String transactionIdOrReason;
        private final String orderId;

        public PaymentResult(boolean success, String transactionIdOrReason, String orderId) {
            this.success = success;
            this.transactionIdOrReason = transactionIdOrReason;
            this.orderId = orderId;
        }

        public boolean isSuccess()            { return success; }
        public String getTransactionId()      { return success ? transactionIdOrReason : null; }
        public String getFailureReason()      { return success ? null : transactionIdOrReason; }
        public String getOrderId()            { return orderId; }

        @Override
        public String toString() {
            return success
                ? "PaymentResult[SUCCESS | txnId=" + transactionIdOrReason + " | order=" + orderId + "]"
                : "PaymentResult[FAILED  | reason=" + transactionIdOrReason + " | order=" + orderId + "]";
        }
    }
}
