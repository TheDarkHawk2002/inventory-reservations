import com.inventory.InputValidator;
import com.inventory.InventoryItem;
import com.inventory.Reservation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {
    @Test
    void shouldRejectNegativeQuantities() {
        assertThrows(IllegalArgumentException.class,
                () -> new InventoryItem("ITEM-1", "Mouse", -1));
        assertThrows(IllegalArgumentException.class,
                () -> new Reservation("RES-1", "ITEM-1", 0));
    }

    @Test
    void shouldRejectMissingIdentifiers() {
        assertThrows(IllegalArgumentException.class,
                () -> new InventoryItem(" ", "Mouse", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Reservation("RES-1", "", 1));
    }

    @Test
    void shouldAcceptValidInput() {
        Map<String, InventoryItem> inventory = new LinkedHashMap<>();
        inventory.put("ITEM-1", new InventoryItem("ITEM-1", "Mouse", 5));
        Map<String, Reservation> reservations = new LinkedHashMap<>();
        reservations.put("RES-1", new Reservation("RES-1", "ITEM-1", 2));

        InputValidator validator = new InputValidator();
        assertTrue(validator.validateInventory(inventory).isEmpty());
        assertTrue(validator.validateReservations(reservations, inventory).isEmpty());
        assertTrue(validator.validateReservations(
                java.util.Collections.singletonMap("RES-2", new Reservation("RES-2", "UNKNOWN", 1)), inventory)
                .stream().anyMatch(message -> message.contains("unknown item")));
    }
}
