import com.inventory.InputValidator;
import com.inventory.InventoryItem;
import com.inventory.Reservation;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {
    @Test
    void shouldAcceptValidInput() {
        InputValidator validator = new InputValidator();
        Map<String, InventoryItem> inventory = new HashMap<>();
        inventory.put("ITEM-1", new InventoryItem("ITEM-1", "Mouse", 5));
        Map<String, Reservation> reservations = new HashMap<>();
        reservations.put("RES-1", new Reservation("RES-1", "ITEM-1", 2));
        assertTrue(validator.validateInventory(inventory).isEmpty());
        assertTrue(validator.validateReservations(reservations).isEmpty());
    }

    @Test
    void shouldRejectNullMaps() {
        InputValidator validator = new InputValidator();
        assertFalse(validator.validateInventory(null).isEmpty());
        assertFalse(validator.validateReservations(null).isEmpty());
    }

    @Test
    void shouldAcceptEmptyMaps() {
        InputValidator validator = new InputValidator();
        assertTrue(validator.validateInventory(Collections.emptyMap()).isEmpty());
        assertTrue(validator.validateReservations(Collections.emptyMap()).isEmpty());
    }
}
