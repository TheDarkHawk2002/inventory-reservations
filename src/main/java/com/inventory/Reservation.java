package com.inventory;

import java.util.Objects;

public class Reservation {
    public enum Status { ACTIVE, RELEASED, EXPIRED }

    private final String reservationId;
    private final String itemId;
    private final int quantity;
    private Status status;

    public Reservation(String reservationId, String itemId, int quantity) {
        this(reservationId, itemId, quantity, Status.ACTIVE);
    }

    public Reservation(String reservationId, String itemId, int quantity, Status status) {
        this.reservationId = requireText(reservationId, "reservationId");
        this.itemId = requireText(itemId, "itemId");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        this.quantity = quantity;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public String getReservationId() { return reservationId; }
    public String getItemId() { return itemId; }
    public int getQuantity() { return quantity; }
    public Status getStatus() { return status; }

    void changeStatus(Status newStatus) {
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        if (status != Status.ACTIVE) throw new IllegalStateException("Only active reservations can change status");
        if (newStatus == Status.ACTIVE) throw new IllegalArgumentException("New status must be terminal");
        status = newStatus;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.trim().isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
