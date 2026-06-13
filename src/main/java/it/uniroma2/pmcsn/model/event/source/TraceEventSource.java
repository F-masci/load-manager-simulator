package it.uniroma2.pmcsn.model.event.source;

import it.uniroma2.pmcsn.model.Job;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An EventSource that reads job definitions from a trace file.
 */
public class TraceEventSource implements EventSource {
    private final String filePath;
    private final List<TraceEntry> traceEntries = new ArrayList<>();
    private int currentIndex = 0;

    private static class TraceEntry {
        final double arrivalTime;
        final double serviceTime;

        TraceEntry(double arrivalTime, double serviceTime) {
            this.arrivalTime = arrivalTime;
            this.serviceTime = serviceTime;
        }
    }

    public TraceEventSource(String filePath) throws IOException {
        this.filePath = filePath;
        loadTrace();
    }

    private void loadTrace() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] tokens = line.split("[,\\s\\t]+");
                if (tokens.length >= 2) {
                    try {
                        double arrival = Double.parseDouble(tokens[0]);
                        double service = Double.parseDouble(tokens[1]);
                        traceEntries.add(new TraceEntry(arrival, service));
                    } catch (NumberFormatException e) {
                        // Skip malformed lines
                    }
                }
            }
        }
    }

    @Override
    public Job getNextJob(double lastArrivalTime) {
        if (currentIndex >= traceEntries.size()) {
            return null;
        }
        TraceEntry entry = traceEntries.get(currentIndex);
        currentIndex++;
        return new Job(currentIndex, entry.arrivalTime, entry.serviceTime);
    }

    @Override
    public void reset() {
        currentIndex = 0;
    }

    @Override
    public long getSeed() {
        return 0L; // TraceEventSource does not use a random number generator, so we return a fixed seed
    }

    @Override
    public void plantSeeds(long seed) {
        // TraceEventSource does not use a random number generator, so this method has no effect
    }
}
