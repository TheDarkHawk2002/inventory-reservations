import com.inventory.InventoryItem;
import com.inventory.InventoryService;
import com.inventory.Reservation;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InventoryServiceTest {
    private InventoryService serviceWithStock(int quantity) {
        Map<String, InventoryItem> inventory = new HashMap<>();
        inventory.put("ITEM-1", new InventoryItem("ITEM-1", "Keyboard", quantity));
        return new InventoryService(inventory, new HashMap<>());
    }

    @Test
    void shouldReserveAvailableStock() {
        InventoryService service = serviceWithStock(10);
        Reservation reservation = service.reserve("RES-1", "ITEM-1", 4);
        assertEquals(Reservation.Status.ACTIVE, reservation.getStatus());
        assertEquals(6, service.getInventory().get("ITEM-1").getQuantity());
    }

    @Test
    void shouldRejectOversizedReservationWithoutChangingStock() {
        InventoryService service = serviceWithStock(3);
        assertThrows(IllegalStateException.class, () -> service.reserve("RES-1", "ITEM-1", 4));
        assertEquals(3, service.getInventory().get("ITEM-1").getQuantity());
        assertTrue(service.getReservations().isEmpty());
    }

    @Test
    void shouldRestoreStockOnReleaseOnlyOnce() {
        InventoryService service = serviceWithStock(10);
        service.reserve("RES-1", "ITEM-1", 4);
        service.release("RES-1");
        assertEquals(10, service.getInventory().get("ITEM-1").getQuantity());
        assertEquals(Reservation.Status.RELEASED, service.getReservations().get("RES-1").getStatus());
        assertThrows(IllegalStateException.class, () -> service.release("RES-1"));
        assertEquals(10, service.getInventory().get("ITEM-1").getQuantity());
    }

    @Test
    void shouldRejectDuplicateReservationId() {
        InventoryService service = serviceWithStock(10);
        service.reserve("RES-1", "ITEM-1", 2);
        assertThrows(IllegalArgumentException.class, () -> service.reserve("RES-1", "ITEM-1", 2));
        assertEquals(8, service.getInventory().get("ITEM-1").getQuantity());
        assertEquals(1, service.getReservations().size());
    }

    @Test
    void shouldRejectNonPositiveQuantity() {
        InventoryService service = serviceWithStock(10);
        assertThrows(IllegalArgumentException.class, () -> service.reserve("RES-1", "ITEM-1", 0));
        assertThrows(IllegalArgumentException.class, () -> service.reserve("RES-2", "ITEM-1", -1));
    }

    @Test
    void shouldRejectUnknownItemsAndReservations() {
        InventoryService service = serviceWithStock(10);
        assertThrows(IllegalArgumentException.class, () -> service.reserve("RES-1", "missing", 1));
        assertThrows(IllegalArgumentException.class, () -> service.release("missing"));
    }
}
