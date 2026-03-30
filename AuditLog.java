package com.ecommerce.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditLog {
	private static final List<String> logs = new ArrayList<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
 
    public static void log(String message) {
        String entry = "[" + LocalDateTime.now().format(FMT) + "] " + message;
        logs.add(entry); // immutable — only append
        System.out.println("  LOG: " + entry);
    }
 
    public static List<String> getLogs() {
        return Collections.unmodifiableList(logs);
    }
 
    public static void printAll() {
        if (logs.isEmpty()) { System.out.println("  No logs yet."); return; }
        logs.forEach(l -> System.out.println("  " + l));
    }
}
