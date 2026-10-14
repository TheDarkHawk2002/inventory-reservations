package com.inventory;
public class Reservation {

    public enum Status {
        ACTIVE,
        RELEASED,
        EXPIRED
    }

    private final String reservationId;
    private final String itemId;
    private final int quantity;
    private Status status;

    public Reservation(
            String reservationId,
            String itemId,
            int quantity) {
        // TODO: IDs und Menge validieren
        this.reservationId = reservationId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.status = Status.ACTIVE;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Status getStatus() {
        return status;
    }

    // TODO: Status einmalig ändern
}