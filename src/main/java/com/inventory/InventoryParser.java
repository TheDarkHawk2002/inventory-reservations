package com.inventory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class InventoryParser {
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, InventoryItem> loadInventory(Path path) throws IOException {
        JsonNode root = readArray(path);
        Map<String, InventoryItem> inventory = new LinkedHashMap<>();
        for (JsonNode node : root) {
            InventoryItem item = new InventoryItem(
                    requiredText(node, "itemId"),
                    requiredText(node, "name"),
                    requiredInt(node, "quantity"));
            if (inventory.putIfAbsent(item.getItemId(), item) != null) {
                throw new IOException("Duplicate itemId in inventory: " + item.getItemId());
            }
        }
        return inventory;
    }

    public Map<String, Reservation> loadReservations(Path path) throws IOException {
        JsonNode root = readArray(path);
        Map<String, Reservation> reservations = new LinkedHashMap<>();
        for (JsonNode node : root) {
            String id = requiredText(node, "reservationId");
            String itemId = requiredText(node, "itemId");
            int quantity = requiredInt(node, "quantity");
            Reservation.Status status = Reservation.Status.ACTIVE;
            if (node.hasNonNull("status")) {
                try {
                    status = Reservation.Status.valueOf(node.get("status").asText().trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    throw new IOException("Invalid status for reservation " + id, ex);
                }
            }
            Reservation reservation = new Reservation(id, itemId, quantity, status);
            if (reservations.putIfAbsent(id, reservation) != null) {
                throw new IOException("Duplicate reservationId: " + id);
            }
        }
        return reservations;
    }

    private JsonNode readArray(Path path) throws IOException {
        if (path == null) throw new IllegalArgumentException("path must not be null");
        if (!Files.isRegularFile(path)) throw new IOException("Input file does not exist: " + path);
        JsonNode root;
        try {
            root = mapper.readTree(path.toFile());
        } catch (JsonProcessingException ex) {
            throw new IOException("Invalid JSON in " + path + ": " + ex.getOriginalMessage(), ex);
        }
        if (root == null || !root.isArray()) throw new IOException("Expected a JSON array in " + path);
        return root;
    }

    private static String requiredText(JsonNode node, String field) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().trim().isEmpty()) {
            throw new IOException("Missing or invalid field: " + field);
        }
        return value.asText().trim();
    }

    private static int requiredInt(JsonNode node, String field) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || !value.canConvertToInt() || !value.isIntegralNumber()) {
            throw new IOException("Missing or invalid integer field: " + field);
        }
        return value.asInt();
    }
}
