package com.inventory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Objects;

public class EventLogger {
    private final Path logPath;

    public EventLogger(Path logPath) {
        this.logPath = Objects.requireNonNull(logPath, "logPath must not be null");
    }

    public synchronized void log(String eventType, String reservationId) throws IOException {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }
        if (reservationId == null || reservationId.trim().isEmpty()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        Path parent = logPath.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        String line = Instant.now() + "\t" + sanitize(eventType) + "\t" + sanitize(reservationId) + System.lineSeparator();
        Files.write(logPath, line.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
    }

    private String sanitize(String value) {
        return value.replace("\\", "\\\\").replace("\t", " ").replace("\r", " ").replace("\n", " ");
    }
}
