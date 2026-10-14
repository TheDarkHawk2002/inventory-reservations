package main.java;
import java.util.Map;

public class InventoryService {

    private final Map<String, InventoryItem> inventory;
    private final Map<String, Reservation> reservations;

    public InventoryService(
            Map<String, InventoryItem> inventory,
            Map<String, Reservation> reservations) {
        this.inventory = inventory;
        this.reservations = reservations;
    }

    // TODO: Bestand und Menge prüfen
    // TODO: Reservierung atomar anlegen
    // TODO: Doppelte IDs verhindern
    // TODO: Reservierungen freigeben
    // TODO: Abgelaufene Reservierungen behandeln
    // TODO: Bestand niemals negativ machen
}