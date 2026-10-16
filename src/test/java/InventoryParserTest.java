import com.inventory.InventoryItem;
import com.inventory.InventoryParser;
import com.inventory.Reservation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InventoryParserTest {
    @TempDir
    Path tempDir;

    private final InventoryParser parser = new InventoryParser();

    @Test
    void shouldLoadValidInventoryFile() throws IOException {
        Map<String, InventoryItem> inventory = parser.loadInventory(Path.of("examples/inventory.json"));

        assertEquals(3, inventory.size());
        assertEquals("Wireless Mouse", inventory.get("ITEM-001").getName());
        assertEquals(25, inventory.get("ITEM-001").getQuantity());
    }

    @Test
    void shouldRejectMalformedJson() throws IOException {
        Path malformed = tempDir.resolve("broken.json");
        Files.write(malformed, "[{\"itemId\":]".getBytes(StandardCharsets.UTF_8));

        IOException error = assertThrows(IOException.class, () -> parser.loadInventory(malformed));
        assertTrue(error.getMessage().contains("Invalid JSON"));
    }

    @Test
    void shouldNotModifySourceFile() throws IOException {
        Path source = Path.of("examples/inventory.json");
        byte[] before = Files.readAllBytes(source);

        parser.loadInventory(source);

        assertArrayEquals(before, Files.readAllBytes(source));
    }

    @Test
    void shouldPersistReservationsWithStableOrder() throws IOException {
        Map<String, Reservation> reservations = new LinkedHashMap<>();
        reservations.put("RES-B", new Reservation("RES-B", "ITEM-002", 1));
        reservations.put("RES-A", new Reservation("RES-A", "ITEM-001", 2));
        Path destination = tempDir.resolve("state/reservations.json");

        parser.saveReservations(destination, reservations);
        Map<String, Reservation> loaded = parser.loadReservations(destination);

        assertEquals(2, loaded.size());
        assertEquals(2, loaded.get("RES-A").getQuantity());
        assertTrue(new String(Files.readAllBytes(destination), StandardCharsets.UTF_8)
                .indexOf("RES-A") < new String(Files.readAllBytes(destination), StandardCharsets.UTF_8)
                .indexOf("RES-B"));
    }
}
