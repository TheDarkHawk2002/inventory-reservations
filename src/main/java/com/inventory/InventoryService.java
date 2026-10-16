package com.inventory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Applies reservation rules while leaving total inventory counts unchanged. */
public final class InventoryService {
    private final Map<String, InventoryItem> inventory;
    private final Map<String, Reservation> reservations;

    public InventoryService(Map<String, InventoryItem> inventory,
                            Map<String, Reservation> reservations) {
        this.inventory = new LinkedHashMap<>(inventory);
        this.reservations = new LinkedHashMap<>(reservations);
    }

    public synchronized Reservation reserve(String reservationId, String itemId,
                                            int quantity, Long expiresInHours, Instant now) {
        String id = requireText(reservationId, "Reservation ID");
        String normalizedItemId = requireText(itemId, "Item ID");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero.");
        if (expiresInHours != null && expiresInHours <= 0) {
            throw new IllegalArgumentException("Expiry duration must be greater than zero hours.");
        }
        if (now == null) throw new IllegalArgumentException("Current time must not be null.");
        if (!inventory.containsKey(normalizedItemId)) {
            throw new IllegalArgumentException("Unknown item: " + normalizedItemId);
        }

        Reservation existing = reservations.get(id);
        if (existing != null) {
            if (existing.getItemId().equals(normalizedItemId) && existing.getQuantity() == quantity) {
                return existing;
            }
            throw new IllegalArgumentException("Reservation ID already exists with different details: " + id);
        }
        int available = getAvailableQuantity(normalizedItemId);
        if (quantity > available) {
            throw new IllegalArgumentException("Insufficient stock for " + normalizedItemId
                    + ": requested " + quantity + ", available " + available + ".");
        }
        Instant expiresAt = expiresInHours == null ? null
                : now.plusSeconds(Math.multiplyExact(expiresInHours, 3600L));
        Reservation reservation = new Reservation(id, normalizedItemId, quantity,
                Reservation.Status.ACTIVE, expiresAt);
        reservations.put(id, reservation);
        return reservation;
    }

    public synchronized Reservation release(String reservationId) {
        String id = requireText(reservationId, "Reservation ID");
        Reservation reservation = reservations.get(id);
        if (reservation == null) throw new IllegalArgumentException("Unknown reservation: " + id);
        reservation.release();
        return reservation;
    }

    public synchronized List<Reservation> expireDue(Instant now) {
        if (now == null) throw new IllegalArgumentException("Current time must not be null.");
        List<Reservation> expired = new ArrayList<>();
        for (Reservation reservation : reservations.values()) {
            if (reservation.isActive() && reservation.getExpiresAt() != null
                    && !reservation.getExpiresAt().isAfter(now)) {
                reservation.expire();
                expired.add(reservation);
            }
        }
        expired.sort(Comparator.comparing(Reservation::getReservationId));
        return expired;
    }

    public synchronized int getAvailableQuantity(String itemId) {
        InventoryItem item = inventory.get(itemId);
        if (item == null) throw new IllegalArgumentException("Unknown item: " + itemId);
        long reserved = 0;
        for (Reservation reservation : reservations.values()) {
            if (reservation.isActive() && reservation.getItemId().equals(itemId)) {
                reserved += reservation.getQuantity();
            }
        }
        long available = item.getQuantity() - reserved;
        if (available < 0) throw new IllegalStateException("Active reservations exceed stock for " + itemId);
        return (int) available;
    }

    public synchronized Map<String, InventoryItem> inventorySnapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(inventory));
    }

    public synchronized Map<String, Reservation> reservationSnapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(reservations));
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + " must not be blank.");
        return value.trim();
    }
}
