package com.inventory;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Command-line entry point. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) System.exit(exitCode);
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0 || "help".equals(args[0]) || "--help".equals(args[0])) {
            printUsage(out);
            return 0;
        }
        String command = args[0].toLowerCase(Locale.ROOT);
        if (!isSupportedCommand(command)) {
            err.println("Unknown command: " + command);
            printUsage(err);
            return 2;
        }
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException ex) {
            err.println("Usage error: " + ex.getMessage());
            printUsage(err);
            return 2;
        }
        try {
            ensureDistinctPaths(options);
            InventoryParser parser = new InventoryParser();
            Map<String, InventoryItem> inventory = parser.loadInventory(options.inventoryPath);
            Map<String, Reservation> reservations = parser.loadReservations(options.reservationsPath);

            InputValidator validator = new InputValidator();
            List<String> errors = validator.validateInventory(inventory);
            if (!errors.isEmpty()) throw new IOException(String.join(System.lineSeparator(), errors));

            InventoryService service = new InventoryService(inventory, reservations);
            List<Reservation> expired = service.expireDue(Instant.now());
            if (!expired.isEmpty()) {
                parser.saveReservations(options.reservationsPath, service.reservationSnapshot());
                EventLogger logger = new EventLogger(options.logPath);
                for (Reservation reservation : expired) logWithWarning(logger, "EXPIRED", reservation, err);
            }
            errors = validator.validateReservations(service.reservationSnapshot(), inventory);
            if (!errors.isEmpty()) throw new IOException(String.join(System.lineSeparator(), errors));

            switch (command) {
                case "list":
                    requirePositionals(options, 0, "list does not take positional arguments");
                    out.print(new ReportWriter().render(service, "text", Instant.now()));
                    return 0;
                case "reserve": return reserve(options, parser, service, out, err);
                case "release": return release(options, parser, service, out, err);
                case "report": return report(options, service, out);
                default: throw new IllegalStateException("Command routing failed.");
            }
        } catch (IOException | IllegalArgumentException | ArithmeticException ex) {
            err.println("Error: " + ex.getMessage());
            return 1;
        }
    }

    private static int reserve(Options options, InventoryParser parser, InventoryService service,
                               PrintStream out, PrintStream err) throws IOException {
        requirePositionals(options, 3, "reserve requires <reservation-id> <item-id> <quantity>");
        int quantity = parsePositiveInt(options.positionals.get(2), "quantity");
        String id = options.positionals.get(0);
        boolean existed = service.reservationSnapshot().containsKey(id);
        Reservation reservation = service.reserve(id, options.positionals.get(1), quantity,
                options.expiresInHours, Instant.now());
        if (existed) {
            out.println("Reservation already exists; no changes made (" + reservation.getStatus() + ").");
            return 0;
        }
        parser.saveReservations(options.reservationsPath, service.reservationSnapshot());
        logWithWarning(new EventLogger(options.logPath), "RESERVED", reservation, err);
        out.println("Reserved " + quantity + " of " + reservation.getItemId()
                + " as " + reservation.getReservationId() + ". Available: "
                + service.getAvailableQuantity(reservation.getItemId()) + ".");
        return 0;
    }

    private static int release(Options options, InventoryParser parser, InventoryService service,
                               PrintStream out, PrintStream err) throws IOException {
        requirePositionals(options, 1, "release requires <reservation-id>");
        String id = options.positionals.get(0);
        Reservation existing = service.reservationSnapshot().get(id);
        if (existing == null) throw new IllegalArgumentException("Unknown reservation: " + id);
        boolean wasActive = existing.isActive();
        Reservation reservation = service.release(id);
        if (!wasActive) {
            out.println("Reservation is already " + reservation.getStatus() + "; no changes made.");
            return 0;
        }
        parser.saveReservations(options.reservationsPath, service.reservationSnapshot());
        logWithWarning(new EventLogger(options.logPath), "RELEASED", reservation, err);
        out.println("Released " + id + ". Available: "
                + service.getAvailableQuantity(reservation.getItemId()) + ".");
        return 0;
    }

    private static int report(Options options, InventoryService service, PrintStream out) throws IOException {
        requirePositionals(options, 0, "report does not take positional arguments");
        String content = new ReportWriter().render(service, options.format, Instant.now());
        if (options.outputPath == null) out.print(content);
        else {
            new ReportWriter().writeReport(content, options.outputPath);
            out.println("Report written to " + options.outputPath.toAbsolutePath().normalize());
        }
        return 0;
    }

    private static void logWithWarning(EventLogger logger, String type,
                                      Reservation reservation, PrintStream err) {
        try {
            logger.log(type, reservation);
        } catch (IOException ex) {
            err.println("Warning: reservation state was saved, but the event log could not be written: "
                    + ex.getMessage());
        }
    }

    private static void ensureDistinctPaths(Options options) {
        Path inventory = normalize(options.inventoryPath);
        Path reservations = normalize(options.reservationsPath);
        Path log = normalize(options.logPath);
        if (inventory.equals(reservations) || inventory.equals(log) || reservations.equals(log)) {
            throw new IllegalArgumentException("Inventory, reservations, and log must use different files.");
        }
        if (options.outputPath != null) {
            Path output = normalize(options.outputPath);
            if (output.equals(inventory) || output.equals(reservations) || output.equals(log)) {
                throw new IllegalArgumentException("Report output must not overwrite an input or log file.");
            }
        }
    }

    private static Path normalize(Path path) { return path.toAbsolutePath().normalize(); }

    private static int parsePositiveInt(String value, String label) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a positive whole number: " + value);
        }
    }

    private static void requirePositionals(Options options, int count, String message) {
        if (options.positionals.size() != count) throw new IllegalArgumentException(message);
    }

    private static boolean isSupportedCommand(String command) {
        return command.equals("list") || command.equals("reserve") || command.equals("release")
                || command.equals("report") || command.equals("help");
    }

    private static void printUsage(PrintStream out) {
        out.println("Inventory Reservations CLI");
        out.println();
        out.println("Commands:");
        out.println("  list");
        out.println("  reserve <reservation-id> <item-id> <quantity> [--expires-in-hours <hours>]");
        out.println("  release <reservation-id>");
        out.println("  report [--format text|csv|json] [--output <file>]");
        out.println();
        out.println("Options (may follow the command):");
        out.println("  --inventory <file>     Inventory JSON (default: examples/inventory.json)");
        out.println("  --reservations <file>  Mutable state (default: data/reservations.json)");
        out.println("  --log <file>           JSONL audit log (default: logs/events.jsonl)");
        out.println("  --expires-in-hours <n> Automatically expire a reservation after n hours");
        out.println("  --format <format>      Report format: text, csv, or json");
        out.println("  --output <file>        Write a report without changing source data");
        out.println();
        out.println("Exit codes: 0 success, 1 data/operation error, 2 command usage error.");
    }

    private static final class Options {
        private Path inventoryPath = Path.of("examples/inventory.json");
        private Path reservationsPath = Path.of("data/reservations.json");
        private Path logPath = Path.of("logs/events.jsonl");
        private Path outputPath;
        private String format = "text";
        private Long expiresInHours;
        private final List<String> positionals = new ArrayList<>();

        private static Options parse(String[] args) {
            Options result = new Options();
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if (!arg.startsWith("--")) {
                    result.positionals.add(arg);
                    continue;
                }
                if (i + 1 >= args.length) throw new IllegalArgumentException("Missing value for " + arg);
                String value = args[++i];
                switch (arg) {
                    case "--inventory": result.inventoryPath = Path.of(value); break;
                    case "--reservations": result.reservationsPath = Path.of(value); break;
                    case "--log": result.logPath = Path.of(value); break;
                    case "--output": result.outputPath = Path.of(value); break;
                    case "--format": result.format = value; break;
                    case "--expires-in-hours":
                        try { result.expiresInHours = Long.parseLong(value); }
                        catch (NumberFormatException ex) {
                            throw new IllegalArgumentException("Expiry must be a whole number of hours.");
                        }
                        break;
                    default: throw new IllegalArgumentException("Unknown option: " + arg);
                }
            }
            return result;
        }
    }
}
