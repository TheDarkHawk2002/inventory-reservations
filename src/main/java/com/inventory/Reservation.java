package com.inventory;

import java.time.Instant;

/** A stock hold. Active reservations reduce available stock. */
public final class Reservation {
    public enum Status { ACTIVE, RELEASED, EXPIRED }

    private final String reservationId;
    private final String itemId;
    private final int quantity;
    private final Instant expiresAt;
    private Status status;

    public Reservation(String reservationId, String itemId, int quantity,
                       Status status, Instant expiresAt) {
        this.reservationId = requireText(reservationId, "reservationId");
        this.itemId = requireText(itemId, "itemId");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be greater than zero");
        if (status == null) throw new IllegalArgumentException("status must not be null");
        this.quantity = quantity;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public Reservation(String reservationId, String itemId, int quantity) {
        this(reservationId, itemId, quantity, Status.ACTIVE, null);
    }

    public String getReservationId() { return reservationId; }
    public String getItemId() { return itemId; }
    public int getQuantity() { return quantity; }
    public Status getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isActive() { return status == Status.ACTIVE; }

    void release() {
        if (isActive()) status = Status.RELEASED;
    }

    void expire() {
        if (isActive()) status = Status.EXPIRED;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
