package com.inventory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class InventoryService {
    private final Map<String, InventoryItem> inventory;
    private final Map<String, Reservation> reservations;

    public InventoryService(Map<String, InventoryItem> inventory, Map<String, Reservation> reservations) {
        this.inventory = new LinkedHashMap<>(Objects.requireNonNull(inventory, "inventory"));
        this.reservations = new LinkedHashMap<>(Objects.requireNonNull(reservations, "reservations"));
        validateKeys(this.inventory, this.reservations);
    }

    public synchronized Reservation reserve(String reservationId, String itemId, int quantity) {
        requireText(reservationId, "reservationId");
        requireText(itemId, "itemId");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (reservations.containsKey(reservationId)) {
            throw new IllegalArgumentException("Reservation ID already exists: " + reservationId);
        }
        InventoryItem item = inventory.get(itemId);
        if (item == null) throw new IllegalArgumentException("Unknown item: " + itemId);
        if (item.getQuantity() < quantity) throw new IllegalStateException("Insufficient stock for item: " + itemId);

        // Validate everything before mutating either map or stock.
        Reservation reservation = new Reservation(reservationId, itemId, quantity);
        item.reduceStock(quantity);
        reservations.put(reservationId, reservation);
        return reservation;
    }

    public synchronized Reservation release(String reservationId) {
        Reservation reservation = requireReservation(reservationId);
        if (reservation.getStatus() != Reservation.Status.ACTIVE) {
            throw new IllegalStateException("Reservation is not active: " + reservationId);
        }
        InventoryItem item = inventory.get(reservation.getItemId());
        if (item == null) throw new IllegalStateException("Reservation refers to unknown item: " + reservation.getItemId());
        item.addStock(reservation.getQuantity());
        reservation.changeStatus(Reservation.Status.RELEASED);
        return reservation;
    }

    public synchronized Reservation expire(String reservationId) {
        Reservation reservation = requireReservation(reservationId);
        if (reservation.getStatus() != Reservation.Status.ACTIVE) {
            throw new IllegalStateException("Reservation is not active: " + reservationId);
        }
        InventoryItem item = inventory.get(reservation.getItemId());
        if (item == null) throw new IllegalStateException("Reservation refers to unknown item: " + reservation.getItemId());
        item.addStock(reservation.getQuantity());
        reservation.changeStatus(Reservation.Status.EXPIRED);
        return reservation;
    }

    public synchronized Map<String, InventoryItem> getInventory() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(inventory));
    }

    public synchronized Map<String, Reservation> getReservations() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(reservations));
    }

    private Reservation requireReservation(String id) {
        requireText(id, "reservationId");
        Reservation reservation = reservations.get(id);
        if (reservation == null) throw new IllegalArgumentException("Unknown reservation: " + id);
        return reservation;
    }

    private static void validateKeys(Map<String, InventoryItem> inventory, Map<String, Reservation> reservations) {
        for (Map.Entry<String, InventoryItem> entry : inventory.entrySet()) {
            if (entry.getValue() == null || !entry.getKey().equals(entry.getValue().getItemId())) {
                throw new IllegalArgumentException("Inventory map keys must match item IDs");
            }
        }
        for (Map.Entry<String, Reservation> entry : reservations.entrySet()) {
            if (entry.getValue() == null || !entry.getKey().equals(entry.getValue().getReservationId())) {
                throw new IllegalArgumentException("Reservation map keys must match reservation IDs");
            }
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.trim().isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
