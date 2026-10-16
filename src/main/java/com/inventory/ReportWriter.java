package com.inventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Creates deterministic text, CSV, and JSON inventory snapshots. */
public final class ReportWriter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public String render(InventoryService service, String format, Instant generatedAt) {
        if (service == null || generatedAt == null) throw new IllegalArgumentException("Report input must not be null.");
        switch (format.toLowerCase(Locale.ROOT)) {
            case "text": return renderText(service, generatedAt);
            case "csv": return renderCsv(service);
            case "json": return renderJson(service, generatedAt);
            default: throw new IllegalArgumentException("Unsupported report format: " + format
                    + " (choose text, csv, or json)");
        }
    }

    public void writeReport(String content, Path outputPath) throws IOException {
        if (content == null || outputPath == null) throw new IllegalArgumentException("Report output must not be null.");
        Path absolute = outputPath.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
        Path temporary = Files.createTempFile(absolute.getParent(), absolute.getFileName().toString(), ".tmp");
        try {
            Files.write(temporary, content.getBytes(StandardCharsets.UTF_8));
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

    private String renderText(InventoryService service, Instant generatedAt) {
        StringBuilder out = new StringBuilder("Inventory overview (generated ")
                .append(generatedAt).append(")\n\n");
        out.append(String.format("%-14s %-24s %8s %10s %10s%n",
                "ITEM", "NAME", "TOTAL", "RESERVED", "AVAILABLE"));
        for (InventoryItem item : sortedItems(service)) {
            int available = service.getAvailableQuantity(item.getItemId());
            out.append(String.format("%-14s %-24s %8d %10d %10d%n", item.getItemId(),
                    item.getName(), item.getQuantity(), item.getQuantity() - available, available));
        }
        out.append("\nReservations\n");
        List<Reservation> reservations = sortedReservations(service);
        if (reservations.isEmpty()) out.append("No reservations.\n");
        for (Reservation reservation : reservations) {
            out.append(reservation.getReservationId()).append(" | ")
                    .append(reservation.getItemId()).append(" | qty ")
                    .append(reservation.getQuantity()).append(" | ")
                    .append(reservation.getStatus());
            if (reservation.getExpiresAt() != null) out.append(" | expires ").append(reservation.getExpiresAt());
            out.append('\n');
        }
        return out.toString();
    }

    private String renderCsv(InventoryService service) {
        StringBuilder out = new StringBuilder(
                "recordType,id,name,totalQuantity,reservedQuantity,availableQuantity,status,expiresAt\n");
        for (InventoryItem item : sortedItems(service)) {
            int available = service.getAvailableQuantity(item.getItemId());
            out.append("inventory,").append(csv(item.getItemId())).append(',')
                    .append(csv(item.getName())).append(',').append(item.getQuantity()).append(',')
                    .append(item.getQuantity() - available).append(',').append(available).append(",,\n");
        }
        for (Reservation reservation : sortedReservations(service)) {
            out.append("reservation,").append(csv(reservation.getReservationId())).append(',')
                    .append(csv(reservation.getItemId())).append(",,,,")
                    .append(reservation.getStatus()).append(',')
                    .append(reservation.getExpiresAt() == null ? "" : csv(reservation.getExpiresAt().toString()))
                    .append('\n');
        }
        return out.toString();
    }

    private String renderJson(InventoryService service, Instant generatedAt) {
        JsonObject root = new JsonObject();
        root.addProperty("generatedAt", generatedAt.toString());
        JsonArray items = new JsonArray();
        for (InventoryItem item : sortedItems(service)) {
            int available = service.getAvailableQuantity(item.getItemId());
            JsonObject row = new JsonObject();
            row.addProperty("itemId", item.getItemId());
            row.addProperty("name", item.getName());
            row.addProperty("totalQuantity", item.getQuantity());
            row.addProperty("reservedQuantity", item.getQuantity() - available);
            row.addProperty("availableQuantity", available);
            items.add(row);
        }
        JsonArray reservationRows = new JsonArray();
        for (Reservation reservation : sortedReservations(service)) {
            JsonObject row = new JsonObject();
            row.addProperty("reservationId", reservation.getReservationId());
            row.addProperty("itemId", reservation.getItemId());
            row.addProperty("quantity", reservation.getQuantity());
            row.addProperty("status", reservation.getStatus().name());
            if (reservation.getExpiresAt() == null) row.add("expiresAt", null);
            else row.addProperty("expiresAt", reservation.getExpiresAt().toString());
            reservationRows.add(row);
        }
        root.add("inventory", items);
        root.add("reservations", reservationRows);
        return GSON.toJson(root) + System.lineSeparator();
    }

    private List<InventoryItem> sortedItems(InventoryService service) {
        List<InventoryItem> items = new ArrayList<>(service.inventorySnapshot().values());
        items.sort(Comparator.comparing(InventoryItem::getItemId));
        return items;
    }

    private List<Reservation> sortedReservations(InventoryService service) {
        List<Reservation> reservations = new ArrayList<>(service.reservationSnapshot().values());
        reservations.sort(Comparator.comparing(Reservation::getReservationId));
        return reservations;
    }

    private String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
