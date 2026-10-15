package com.inventory;

import java.util.Objects;

public class InventoryItem {
    private final String itemId;
    private final String name;
    private int quantity;

    public InventoryItem(String itemId, String name, int quantity) {
        this.itemId = requireText(itemId, "itemId");
        this.name = requireText(name, "name");
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
        this.quantity = quantity;
    }

    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }

    void reduceStock(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (amount > quantity) throw new IllegalStateException("Insufficient stock for item " + itemId);
        quantity -= amount;
    }

    void addStock(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        quantity = Math.addExact(quantity, amount);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.trim().isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
