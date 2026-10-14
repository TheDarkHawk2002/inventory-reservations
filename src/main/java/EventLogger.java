package main.java;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

public class EventLogger {

    private final Path logPath;

    public EventLogger(Path logPath) {
        this.logPath = logPath;
    }

    // TODO: Ereignis mit Zeitstempel speichern
    public void log(String eventType, String reservationId)
            throws IOException {

        Instant timestamp = Instant.now();

        // TODO: Zeitstempel, Ereignis und ID protokollieren
    }
}