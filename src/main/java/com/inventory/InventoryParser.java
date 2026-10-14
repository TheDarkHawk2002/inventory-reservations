package com.inventory;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Map;

public class InventoryParser {

    // TODO: JSON-Datei lesen und parsen
    public Map<String, InventoryItem> loadInventory(Path path)
            throws IOException {
        throw new UnsupportedOperationException(
                "TODO: Inventar aus JSON laden");
    }

    // TODO: Reservierungsdaten aus JSON laden
    public Map<String, Reservation> loadReservations(Path path)
            throws IOException {
        throw new UnsupportedOperationException(
                "TODO: Reservierungen aus JSON laden");
    }
}