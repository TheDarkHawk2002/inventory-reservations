# Inventory Reservations CLI

A small Java command-line application for managing stock reservations. It validates inventory data, prevents over-reservation, persists reservation changes safely, supports release and expiry, and produces readable or machine-friendly reports.

This portfolio project uses synthetic sample data. It is designed to demonstrate backend fundamentals in a compact, easy-to-review codebase.

## Features

- Load and validate inventory and reservation data from JSON.
- Reserve available stock without changing the source inventory file.
- Make repeated requests with the same reservation ID idempotent; reject conflicting reuse.
- Release reservations and automatically expire reservations with an optional expiry time.
- Persist changes through a temporary file and atomic replacement where supported.
- Append reservation events to a JSON Lines audit log.
- Generate text, CSV, and JSON reports with stable ordering.
- Return distinct exit codes for successful commands, operational errors, and usage errors.

## Requirements

- JDK 11 or newer
- Maven 3.6.3 or newer

## Build

```bash
mvn clean package
```

The build creates an executable JAR at `target/inventory-reservations-1.0.0.jar`.

## Quick start

```bash
java -jar target/inventory-reservations-1.0.0.jar --help
java -jar target/inventory-reservations-1.0.0.jar list
java -jar target/inventory-reservations-1.0.0.jar reserve RES-003 ITEM-001 4
java -jar target/inventory-reservations-1.0.0.jar list
java -jar target/inventory-reservations-1.0.0.jar release RES-003
```

By default, the application reads `examples/inventory.json`, stores editable state in `data/reservations.json`, and appends events to `logs/events.jsonl`. The mutable state and log are local and excluded from Git.

Create a reservation that expires after 24 hours:

```bash
java -jar target/inventory-reservations-1.0.0.jar reserve RES-004 ITEM-002 2 --expires-in-hours 24
```

Generate reports:

```bash
java -jar target/inventory-reservations-1.0.0.jar report --format text
java -jar target/inventory-reservations-1.0.0.jar report --format csv --output reports/inventory.csv
java -jar target/inventory-reservations-1.0.0.jar report --format json --output reports/inventory.json
```

Input paths can be supplied per command:

```bash
java -jar target/inventory-reservations-1.0.0.jar list \
  --inventory examples/inventory.json \
  --reservations data/reservations.json \
  --log logs/events.jsonl
```

## Data model and rules

`quantity` in the inventory file is the total on-hand stock. The available quantity is calculated by subtracting all active reservations. Releasing or expiring a reservation therefore restores availability without editing the inventory source file.

Reservation IDs are unique. Repeating a request with the same ID, item, and quantity returns the existing reservation without changing state. Reusing an ID for different details is rejected. All active reservations must reference an existing item and fit within its stock.

The input files are intentionally small JSON arrays so the data model stays easy to inspect:

```json
[
  { "itemId": "ITEM-001", "name": "Wireless Mouse", "quantity": 25 }
]
```

## Code map

| Class | Responsibility |
|---|---|
| `Main` | CLI parsing, command routing, and exit codes |
| `InventoryParser` | Strict JSON loading and atomic reservation persistence |
| `InputValidator` | Cross-record and stock consistency checks |
| `InventoryService` | Reservation, release, availability, and expiry rules |
| `EventLogger` | Append-only JSONL event history |
| `ReportWriter` | Stable text, CSV, and JSON snapshots |

## Scope

This is a local, single-user demonstration. It does not include a database, authentication, concurrent multi-process locking, or a network API. Those would be sensible next steps for a production service, but are intentionally outside this small portfolio project.

## License

No license has been selected yet. Add one before accepting external contributions or inviting reuse.
