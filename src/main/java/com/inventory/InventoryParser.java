package com.inventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Reads inventory data and atomically persists reservation state. */
public final class InventoryParser {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public Map<String, InventoryItem> loadInventory(Path path) throws IOException {
        JsonArray rows = readArray(path, false);
        Map<String, InventoryItem> inventory = new LinkedHashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            JsonObject row = requireObject(rows.get(i), path, i);
            String id = requiredString(row, "itemId", path, i);
            String name = requiredString(row, "name", path, i);
            int quantity = requiredInt(row, "quantity", path, i);
            try {
                InventoryItem item = new InventoryItem(id, name, quantity);
                if (inventory.putIfAbsent(id, item) != null) {
                    throw dataError(path, i, "duplicate itemId " + id);
                }
            } catch (IllegalArgumentException ex) {
                throw dataError(path, i, ex.getMessage(), ex);
            }
        }
        return inventory;
    }

    public Map<String, Reservation> loadReservations(Path path) throws IOException {
        JsonArray rows = readArray(path, true);
        Map<String, Reservation> reservations = new LinkedHashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            JsonObject row = requireObject(rows.get(i), path, i);
            String id = requiredString(row, "reservationId", path, i);
            String itemId = requiredString(row, "itemId", path, i);
            int quantity = requiredInt(row, "quantity", path, i);
            String rawStatus = requiredString(row, "status", path, i);
            Reservation.Status status;
            try {
                status = Reservation.Status.valueOf(rawStatus.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw dataError(path, i, "unknown reservation status " + rawStatus, ex);
            }
            Instant expiresAt = null;
            if (row.has("expiresAt") && !row.get("expiresAt").isJsonNull()) {
                String rawExpiry = requiredString(row, "expiresAt", path, i);
                try {
                    expiresAt = Instant.parse(rawExpiry);
                } catch (DateTimeParseException ex) {
                    throw dataError(path, i, "expiresAt must be an ISO-8601 timestamp", ex);
                }
            }
            try {
                Reservation reservation = new Reservation(id, itemId, quantity, status, expiresAt);
                if (reservations.putIfAbsent(id, reservation) != null) {
                    throw dataError(path, i, "duplicate reservationId " + id);
                }
            } catch (IllegalArgumentException ex) {
                throw dataError(path, i, ex.getMessage(), ex);
            }
        }
        return reservations;
    }

    public void saveReservations(Path path, Map<String, Reservation> reservations) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        Files.createDirectories(parent);
        JsonArray rows = new JsonArray();
        for (Reservation reservation : new TreeMap<>(reservations).values()) {
            JsonObject row = new JsonObject();
            row.addProperty("reservationId", reservation.getReservationId());
            row.addProperty("itemId", reservation.getItemId());
            row.addProperty("quantity", reservation.getQuantity());
            row.addProperty("status", reservation.getStatus().name());
            if (reservation.getExpiresAt() == null) row.add("expiresAt", null);
            else row.addProperty("expiresAt", reservation.getExpiresAt().toString());
            rows.add(row);
        }
        Path temporary = Files.createTempFile(parent, absolute.getFileName().toString(), ".tmp");
        try {
            Files.write(temporary, GSON.toJson(rows).getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private JsonArray readArray(Path path, boolean missingMeansEmpty) throws IOException {
        if (missingMeansEmpty && !Files.exists(path)) return new JsonArray();
        if (!Files.isRegularFile(path)) throw new IOException("Not a readable file: " + path);
        try {
            JsonElement root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!root.isJsonArray()) throw new IOException("Expected a JSON array in " + path);
            return root.getAsJsonArray();
        } catch (JsonParseException ex) {
            throw new IOException("Invalid JSON in " + path + ": " + ex.getMessage(), ex);
        }
    }

    private JsonObject requireObject(JsonElement element, Path path, int index) throws IOException {
        if (element == null || !element.isJsonObject()) {
            throw dataError(path, index, "entry must be a JSON object");
        }
        return element.getAsJsonObject();
    }

    private String requiredString(JsonObject object, String field, Path path, int index) throws IOException {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            throw dataError(path, index, "field '" + field + "' must be a string");
        }
        String result = value.getAsString().trim();
        if (result.isEmpty()) throw dataError(path, index, "field '" + field + "' must not be blank");
        return result;
    }

    private int requiredInt(JsonObject object, String field, Path path, int index) throws IOException {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw dataError(path, index, "field '" + field + "' must be an integer");
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        try {
            return new BigDecimal(primitive.getAsString()).intValueExact();
        } catch (NumberFormatException | ArithmeticException ex) {
            throw dataError(path, index, "field '" + field + "' must be an in-range integer", ex);
        }
    }

    private IOException dataError(Path path, int index, String message) {
        return new IOException(path + " entry " + (index + 1) + ": " + message);
    }

    private IOException dataError(Path path, int index, String message, Exception cause) {
        return new IOException(path + " entry " + (index + 1) + ": " + message, cause);
    }
}
