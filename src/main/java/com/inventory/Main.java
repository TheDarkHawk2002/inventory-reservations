package com.inventory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        if (args.length < 3) {
            printUsage();
            System.exit(2);
        }

        try {
            Path inventoryPath = Paths.get(args[0]);
            Path reservationsPath = Paths.get(args[1]);
            String command = args[2].toLowerCase();
            InventoryParser parser = new InventoryParser();
            Map<String, InventoryItem> inventory = parser.loadInventory(inventoryPath);
            Map<String, Reservation> reservations = parser.loadReservations(reservationsPath);
            InventoryService service = new InventoryService(inventory, reservations);
            EventLogger logger = new EventLogger(Paths.get("output", "events.log"));
            List<String> report = new ArrayList<>();

            switch (command) {
                case "list":
                    for (InventoryItem item : service.getInventory().values()) {
                        String line = item.getItemId() + " | " + item.getName() + " | available=" + item.getQuantity();
                        System.out.println(line);
                        report.add(line);
                    }
                    break;
                case "reserve":
                    if (args.length != 6) {
                        printUsage();
                        System.exit(2);
                        return;
                    }
                    Reservation created = service.reserve(args[3], args[4], Integer.parseInt(args[5]));
                    logger.log("RESERVED", created.getReservationId());
                    report.add("Reserved " + created.getQuantity() + " of " + created.getItemId()
                            + " as " + created.getReservationId());
                    System.out.println(report.get(0));
                    break;
                case "release":
                    if (args.length != 4) {
                        printUsage();
                        System.exit(2);
                        return;
                    }
                    Reservation released = service.release(args[3]);
                    logger.log("RELEASED", released.getReservationId());
                    report.add("Released " + released.getReservationId() + "; stock restored for " + released.getItemId());
                    System.out.println(report.get(0));
                    break;
                default:
                    printUsage();
                    System.exit(2);
                    return;
            }

            if (!command.equals("list")) {
                Path output = Paths.get("output");
                parser.saveInventory(service.getInventory(), output.resolve("inventory.json"));
                parser.saveReservations(service.getReservations(), output.resolve("reservations.json"));
            }
            if (!report.isEmpty()) {
                new ReportWriter().writeReport(report, Paths.get("output", "report.txt"), "txt");
            }
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  mvn exec:java -Dexec.args=\"examples/inventory.json examples/reservations.json list\"");
        System.out.println("  mvn exec:java -Dexec.args=\"examples/inventory.json examples/reservations.json reserve RES-003 ITEM-001 2\"");
        System.out.println("  mvn exec:java -Dexec.args=\"output/inventory.json output/reservations.json release RES-003\"");
        System.out.println("Changed state is written under output/; example input files remain untouched.");
    }
}
