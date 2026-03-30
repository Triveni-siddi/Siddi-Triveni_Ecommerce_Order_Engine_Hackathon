package com.ecommerce.model;

public class Product {
	private String id;
    private String name;
    private double price;
    private int stock;
    private int reservedStock;
 
    public Product(String id, String name, double price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.reservedStock = 0;
    }
 
    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public int getReservedStock() { return reservedStock; }
    public int getAvailableStock() { return stock - reservedStock; }
 
    public void setStock(int stock) { this.stock = stock; }
 
    public boolean reserveStock(int qty) {
        if (getAvailableStock() >= qty) {
            reservedStock += qty;
            return true;
        }
        return false;
    }
 
    public void releaseReservation(int qty) {
        reservedStock = Math.max(0, reservedStock - qty);
    }
 
    public void deductStock(int qty) {
        stock -= qty;
        releaseReservation(qty);
    }
 
    public void restoreStock(int qty) {
        stock += qty;
    }
 
    @Override
    public String toString() {
        return String.format("[%s] %s | Price: ₹%.2f | Stock: %d | Reserved: %d | Available: %d",
                id, name, price, stock, reservedStock, getAvailableStock());
    }
}
