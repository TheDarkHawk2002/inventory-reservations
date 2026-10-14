package main.java;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InputValidator {

    // TODO: Artikel und Reservierungen prüfen
    public List<String> validateInventory(
            Map<String, InventoryItem> inventory) {

        List<String> errors = new ArrayList<>();

        // TODO: IDs, Namen und Bestandsmengen prüfen

        return errors;
    }

    public List<String> validateReservations(
            Map<String, Reservation> reservations) {

        List<String> errors = new ArrayList<>();

        // TODO: IDs, Mengen und Status prüfen

        return errors;
    }
}