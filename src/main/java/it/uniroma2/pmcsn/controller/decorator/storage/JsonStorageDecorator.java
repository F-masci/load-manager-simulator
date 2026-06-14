package it.uniroma2.pmcsn.controller.decorator.storage;

import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.controller.decorator.SimulatorDecorator;
import it.uniroma2.pmcsn.controller.decorator.data.DataExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Decorator that persists captured simulation data to a JSON file.
 */
public class JsonStorageDecorator extends SimulatorDecorator {
    private static final Logger logger = LoggerFactory.getLogger(JsonStorageDecorator.class);
    private final String outputPath;

    public JsonStorageDecorator(Simulator decorated, String outputPath) {
        super(decorated);
        this.outputPath = outputPath;
    }

    @Override
    public void finalizeSimulation() {
        super.finalizeSimulation();
        persistData();
    }

    private void persistData() {
        DataExporter exporter = findExporter(decorated);
        if (exporter == null) {
            logger.warn("JsonStorageDecorator: No DataExporter found in the decoration chain.");
            return;
        }

        List<Map<String, Object>> data = exporter.getCapturedData();
        String[] headers = exporter.getHeaders();

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println("[");
            for (int i = 0; i < data.size(); i++) {
                Map<String, Object> snapshot = data.get(i);
                writer.print("  {");
                for (int j = 0; j < headers.length; j++) {
                    Object val = snapshot.get(headers[j]);
                    String fmt = (val instanceof Number) ? "\"%s\": %s" : "\"%s\": \"%s\"";
                    writer.printf(fmt, headers[j], val != null ? val.toString() : "null");
                    if (j < headers.length - 1) writer.print(", ");
                }
                writer.print("}");
                if (i < data.size() - 1) writer.println(",");
                else writer.println();
            }
            writer.println("]");
            logger.info("Simulation state successfully persisted to JSON: {}", outputPath);
        } catch (IOException e) {
            logger.error("Failed to write JSON file: {}", outputPath, e);
        }
    }

    private DataExporter findExporter(Simulator s) {
        if (s instanceof DataExporter de) return de;
        if (s instanceof SimulatorDecorator sd) return findExporter(sd.getDecorated());
        return null;
    }
}
