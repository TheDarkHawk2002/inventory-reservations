package com.inventory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ReportWriter {

    // TODO: JSON-, CSV- oder Textbericht schreiben
    public void writeReport(
            List<String> results,
            Path outputPath,
            String format) throws IOException {

        // TODO: Format prüfen
        // TODO: Ergebnisse stabil sortieren
        // TODO: Neue Zieldatei schreiben
        // TODO: Quelldaten niemals überschreiben
    }
}