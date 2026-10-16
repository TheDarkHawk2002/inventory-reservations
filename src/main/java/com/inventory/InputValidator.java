package com.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Validates loaded data before it can affect stock availability. */
public final class InputValidator {
    public List<String> validateInventory(Map<String, InventoryItem> inventory) {
        List<String> errors = new ArrayList<>();
        if (inventory == null || inventory.isEmpty()) {
            errors.add("Inventory must contain at least one item.");
            return errors;
        }
        for (Map.Entry<String, InventoryItem> entry : inventory.entrySet()) {
            InventoryItem item = entry.getValue();
            if (item == null) {
                errors.add("Inventory entry " + entry.getKey() + " is null.");
            } else if (!entry.getKey().equals(item.getItemId())) {
                errors.add("Inventory key does not match item ID " + item.getItemId() + ".");
            }
        }
        return errors;
    }

    public List<String> validateReservations(Map<String, Reservation> reservations,
                                             Map<String, InventoryItem> inventory) {
        List<String> errors = new ArrayList<>();
        if (reservations == null || inventory == null) {
            errors.add("Reservation and inventory data must not be null.");
            return errors;
        }
        Map<String, Long> activeByItem = new HashMap<>();
        for (Map.Entry<String, Reservation> entry : reservations.entrySet()) {
            Reservation reservation = entry.getValue();
            if (reservation == null) {
                errors.add("Reservation entry " + entry.getKey() + " is null.");
                continue;
            }
            if (!entry.getKey().equals(reservation.getReservationId())) {
                errors.add("Reservation key does not match ID " + reservation.getReservationId() + ".");
            }
            if (!inventory.containsKey(reservation.getItemId())) {
                errors.add("Reservation " + reservation.getReservationId()
                        + " references unknown item " + reservation.getItemId() + ".");
            }
            if (reservation.getQuantity() <= 0) {
                errors.add("Reservation " + reservation.getReservationId() + " must have positive quantity.");
            }
            if (reservation.getStatus() == null) {
                errors.add("Reservation " + reservation.getReservationId() + " has no status.");
            } else if (reservation.isActive()) {
                activeByItem.merge(reservation.getItemId(), (long) reservation.getQuantity(), Long::sum);
            }
        }
        for (Map.Entry<String, Long> active : activeByItem.entrySet()) {
            InventoryItem item = inventory.get(active.getKey());
            if (item != null && active.getValue() > item.getQuantity()) {
                errors.add("Active reservations for " + item.getItemId() + " exceed total stock ("
                        + active.getValue() + "/" + item.getQuantity() + ").");
            }
        }
        return errors;
    }
}
