package com.inventory;

/** Immutable stock item. Quantity represents total on-hand stock. */
public final class InventoryItem {
    private final String itemId;
    private final String name;
    private final int quantity;

    public InventoryItem(String itemId, String name, int quantity) {
        this.itemId = requireText(itemId, "itemId");
        this.name = requireText(name, "name");
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
        this.quantity = quantity;
    }

    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
