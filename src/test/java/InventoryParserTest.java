import com.inventory.InventoryItem;
import com.inventory.InventoryParser;
import com.inventory.Reservation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InventoryParserTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldLoadValidInventoryFile() throws IOException {
        Path file = tempDir.resolve("inventory.json");
        Files.write(file, "[{\"itemId\":\"ITEM-1\",\"name\":\"Mouse\",\"quantity\":7}]".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Map<String, InventoryItem> inventory = new InventoryParser().loadInventory(file);
        assertEquals(1, inventory.size());
        assertEquals(7, inventory.get("ITEM-1").getQuantity());
    }

    @Test
    void shouldRejectMalformedJson() throws IOException {
        Path file = tempDir.resolve("broken.json");
        Files.write(file, "[{broken".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> new InventoryParser().loadInventory(file));
    }

    @Test
    void shouldNotModifySourceFile() throws IOException {
        Path file = tempDir.resolve("inventory.json");
        byte[] original = "[{\"itemId\":\"ITEM-1\",\"name\":\"Mouse\",\"quantity\":7}]".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, original);
        new InventoryParser().loadInventory(file);
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    void shouldLoadReservationStatus() throws IOException {
        Path file = tempDir.resolve("reservations.json");
        Files.write(file, "[{\"reservationId\":\"RES-1\",\"itemId\":\"ITEM-1\",\"quantity\":2,\"status\":\"ACTIVE\"}]".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Map<String, Reservation> reservations = new InventoryParser().loadReservations(file);
        assertEquals(Reservation.Status.ACTIVE, reservations.get("RES-1").getStatus());
    }
}
