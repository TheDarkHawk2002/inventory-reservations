package com.inventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/** Appends one JSON object per line to provide an inspectable audit trail. */
public final class EventLogger {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private final Path logPath;

    public EventLogger(Path logPath) {
        if (logPath == null) throw new IllegalArgumentException("Log path must not be null.");
        this.logPath = logPath;
    }

    public synchronized void log(String eventType, Reservation reservation) throws IOException {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type must not be blank.");
        }
        if (reservation == null) throw new IllegalArgumentException("Reservation must not be null.");
        Path absolute = logPath.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());

        JsonObject event = new JsonObject();
        event.addProperty("timestamp", Instant.now().toString());
        event.addProperty("eventType", eventType);
        event.addProperty("reservationId", reservation.getReservationId());
        event.addProperty("itemId", reservation.getItemId());
        event.addProperty("quantity", reservation.getQuantity());
        event.addProperty("status", reservation.getStatus().name());
        if (reservation.getExpiresAt() == null) event.add("expiresAt", null);
        else event.addProperty("expiresAt", reservation.getExpiresAt().toString());
        Files.write(absolute, (GSON.toJson(event) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
    }
}
