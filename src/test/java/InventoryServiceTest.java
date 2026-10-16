import com.inventory.InventoryItem;
import com.inventory.InventoryService;
import com.inventory.Reservation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InventoryServiceTest {
    private InventoryService createService() {
        Map<String, InventoryItem> inventory = new LinkedHashMap<>();
        inventory.put("ITEM-1", new InventoryItem("ITEM-1", "Mouse", 10));
        return new InventoryService(inventory, new LinkedHashMap<>());
    }

    @Test
    void shouldReserveAvailableStock() {
        InventoryService service = createService();

        Reservation reservation = service.reserve("RES-1", "ITEM-1", 4, null, Instant.EPOCH);

        assertEquals(Reservation.Status.ACTIVE, reservation.getStatus());
        assertEquals(6, service.getAvailableQuantity("ITEM-1"));
    }

    @Test
    void shouldRejectOversizedReservation() {
        InventoryService service = createService();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.reserve("RES-1", "ITEM-1", 11, null, Instant.EPOCH));

        assertTrue(error.getMessage().contains("Insufficient stock"));
        assertTrue(service.reservationSnapshot().isEmpty());
        assertEquals(10, service.getAvailableQuantity("ITEM-1"));
    }

    @Test
    void shouldRestoreStockOnRelease() {
        InventoryService service = createService();
        service.reserve("RES-1", "ITEM-1", 4, null, Instant.EPOCH);

        Reservation released = service.release("RES-1");

        assertEquals(Reservation.Status.RELEASED, released.getStatus());
        assertEquals(10, service.getAvailableQuantity("ITEM-1"));
        assertSame(released, service.release("RES-1"));
    }

    @Test
    void shouldHandleDuplicateReservationIdIdempotently() {
        InventoryService service = createService();
        Reservation first = service.reserve("RES-1", "ITEM-1", 4, null, Instant.EPOCH);

        Reservation repeated = service.reserve("RES-1", "ITEM-1", 4, null, Instant.EPOCH.plusSeconds(60));

        assertSame(first, repeated);
        assertEquals(1, service.reservationSnapshot().size());
        assertEquals(6, service.getAvailableQuantity("ITEM-1"));
    }

    @Test
    void shouldRejectConflictingReservationId() {
        InventoryService service = createService();
        service.reserve("RES-1", "ITEM-1", 4, null, Instant.EPOCH);

        assertThrows(IllegalArgumentException.class,
                () -> service.reserve("RES-1", "ITEM-1", 5, null, Instant.EPOCH));
        assertEquals(6, service.getAvailableQuantity("ITEM-1"));
    }

    @Test
    void shouldExpireTimedOutReservationAndRestoreAvailability() {
        InventoryService service = createService();
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        service.reserve("RES-1", "ITEM-1", 4, 1L, createdAt);

        assertEquals(1, service.expireDue(createdAt.plusSeconds(3600)).size());
        assertEquals(Reservation.Status.EXPIRED,
                service.reservationSnapshot().get("RES-1").getStatus());
        assertEquals(10, service.getAvailableQuantity("ITEM-1"));
    }
}
