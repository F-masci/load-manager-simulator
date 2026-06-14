package it.uniroma2.pmcsn.controller.decorator.storage;

import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.controller.decorator.SimulatorDecorator;
import it.uniroma2.pmcsn.controller.decorator.data.DataExporter;
import it.uniroma2.pmcsn.utils.LogFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Decorator that persists captured simulation data to a CSV file.
 */
public class CsvStorageDecorator extends SimulatorDecorator {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(CsvStorageDecorator.class, "STORAGE");
    private final String outputPath;

    public CsvStorageDecorator(Simulator decorated, String outputPath) {
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
            logger.warn("CsvStorageDecorator: No DataExporter found in the decoration chain.");
            return;
        }

        List<Map<String, Object>> data = exporter.getCapturedData();
        String[] headers = exporter.getHeaders();

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            // Write Header
            writer.println(String.join(",", headers));

            // Write Data
            for (Map<String, Object> snapshot : data) {
                StringBuilder row = new StringBuilder();
                for (int i = 0; i < headers.length; i++) {
                    Object val = snapshot.get(headers[i]);
                    row.append(val != null ? val.toString() : "");
                    if (i < headers.length - 1) row.append(",");
                }
                writer.println(row.toString());
            }
            logger.info("Simulation state successfully persisted to CSV: {}", outputPath);
        } catch (IOException e) {
            logger.error("Failed to write CSV file: {}", outputPath, e);
        }
    }

    private DataExporter findExporter(Simulator s) {
        if (s instanceof DataExporter de) return de;
        if (s instanceof SimulatorDecorator sd) return findExporter(sd.getDecorated());
        return null;
    }
}
