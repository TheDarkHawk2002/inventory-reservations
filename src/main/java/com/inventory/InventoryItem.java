package com.inventory;
public class InventoryItem {

    private final String itemId;
    private final String name;
    private int quantity;

    public InventoryItem(String itemId, String name, int quantity) {
        // TODO: Felder validieren und initialisieren
        this.itemId = itemId;
        this.name = name;
        this.quantity = quantity;
    }

    public String getItemId() {
        return itemId;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    // TODO: Bestand sicher reduzieren
    // TODO: Freigegebene Menge hinzufügen
}