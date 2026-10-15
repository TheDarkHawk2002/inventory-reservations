# Inventory Reservations

A small Java 11 command-line application for managing available stock and reservations.

## Features

- Validates item IDs, names, reservation IDs, and quantities.
- Prevents reservations when stock is insufficient.
- Prevents duplicate reservation IDs.
- Releases active reservations and restores stock exactly once.
- Loads and writes JSON files.
- Writes an append-only event log and a simple report.
- Includes JUnit 5 tests for normal cases and edge cases.

## Requirements

- JDK 11+
- Maven 3.9+

## Run tests

From the project root:

```powershell
mvn clean test
```

## Try the CLI

List inventory:

```powershell
mvn exec:java "-Dexec.args=examples/inventory.json examples/reservations.json list"
```

Create a reservation:

```powershell
mvn exec:java "-Dexec.args=examples/inventory.json examples/reservations.json reserve RES-003 ITEM-001 2"
```

The updated inventory and reservations are written to `output/inventory.json` and `output/reservations.json`. The example files are not modified.

Release a reservation using the generated state files:

```powershell
mvn exec:java "-Dexec.args=output/inventory.json output/reservations.json release RES-003"
```

Output files include `output/events.log` and `output/report.txt`.

## JSON format

Inventory:

```json
[
  {
    "itemId": "ITEM-001",
    "name": "Wireless Mouse",
    "quantity": 25
  }
]
```

Reservations:

```json
[
  {
    "reservationId": "RES-001",
    "itemId": "ITEM-001",
    "quantity": 3,
    "status": "ACTIVE"
  }
]
```

## Design

- `InventoryItem` owns stock and guards against invalid stock changes.
- `Reservation` represents the reservation lifecycle.
- `InventoryService` coordinates reservation and release operations.
- `InventoryParser` handles JSON input/output.
- `InputValidator` checks loaded data.
- `EventLogger` and `ReportWriter` provide basic audit output.

The application keeps state in JSON files rather than a database. It is intended as a small portfolio project, not a distributed inventory system.
