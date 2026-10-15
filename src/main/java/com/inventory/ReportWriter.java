package com.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ReportWriter {
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void writeReport(List<String> results, Path outputPath, String format) throws IOException {
        Objects.requireNonNull(results, "results must not be null");
        Objects.requireNonNull(outputPath, "outputPath must not be null");
        if (format == null) throw new IllegalArgumentException("format must not be null");

        String normalizedFormat = format.trim().toLowerCase();
        if (!normalizedFormat.equals("txt") && !normalizedFormat.equals("json") && !normalizedFormat.equals("csv")) {
            throw new IllegalArgumentException("Unsupported report format: " + format);
        }

        List<String> sorted = new ArrayList<>(results);
        Collections.sort(sorted);
        String content;
        switch (normalizedFormat) {
            case "json":
                content = mapper.writeValueAsString(sorted) + System.lineSeparator();
                break;
            case "csv":
                StringBuilder csv = new StringBuilder("result\n");
                for (String result : sorted) {
                    csv.append('"').append(result.replace("\"", "\"\"")).append('"').append('\n');
                }
                content = csv.toString();
                break;
            default:
                content = String.join(System.lineSeparator(), sorted) + System.lineSeparator();
        }

        Path absolute = outputPath.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.write(absolute, content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }
}
