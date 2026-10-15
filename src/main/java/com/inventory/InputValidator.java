package com.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InputValidator {
    public List<String> validateInventory(Map<String, InventoryItem> inventory) {
        List<String> errors = new ArrayList<>();
        if (inventory == null) {
            errors.add("Inventory must not be null");
            return errors;
        }
        for (Map.Entry<String, InventoryItem> entry : inventory.entrySet()) {
            InventoryItem item = entry.getValue();
            if (item == null) {
                errors.add("Inventory contains a null item for key " + entry.getKey());
                continue;
            }
            if (entry.getKey() == null || !entry.getKey().equals(item.getItemId())) {
                errors.add("Inventory map key does not match itemId: " + entry.getKey());
            }
            if (item.getItemId() == null || item.getItemId().trim().isEmpty()) errors.add("Item ID must not be blank");
            if (item.getName() == null || item.getName().trim().isEmpty()) errors.add("Item name must not be blank");
            if (item.getQuantity() < 0) errors.add("Quantity must not be negative for " + item.getItemId());
        }
        return errors;
    }

    public List<String> validateReservations(Map<String, Reservation> reservations) {
        List<String> errors = new ArrayList<>();
        if (reservations == null) {
            errors.add("Reservations must not be null");
            return errors;
        }
        for (Map.Entry<String, Reservation> entry : reservations.entrySet()) {
            Reservation reservation = entry.getValue();
            if (reservation == null) {
                errors.add("Reservations contains a null value for key " + entry.getKey());
                continue;
            }
            if (entry.getKey() == null || !entry.getKey().equals(reservation.getReservationId())) {
                errors.add("Reservation map key does not match reservationId: " + entry.getKey());
            }
            if (reservation.getReservationId() == null || reservation.getReservationId().trim().isEmpty()) {
                errors.add("Reservation ID must not be blank");
            }
            if (reservation.getItemId() == null || reservation.getItemId().trim().isEmpty()) {
                errors.add("Reservation itemId must not be blank");
            }
            if (reservation.getQuantity() <= 0) errors.add("Reservation quantity must be positive: " + entry.getKey());
            if (reservation.getStatus() == null) errors.add("Reservation status must not be null: " + entry.getKey());
        }
        return errors;
    }
}
