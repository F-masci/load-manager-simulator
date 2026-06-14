package it.uniroma2.pmcsn.simulator;

import it.uniroma2.pmcsn.BaseTest;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Base class for simulation-related tests, providing utilities for trace files management.
 */
public abstract class BaseSimulationTest extends BaseTest {

    @TempDir
    protected Path tempDir;

    /**
     * Creates a temporary trace file with the given content lines.
     * Each line should follow the format: "arrivalTime serviceTime".
     *
     * @param lines List of strings, each representing a job's arrival and service time.
     * @return The absolute path to the generated trace file.
     * @throws IOException If file creation or writing fails.
     */
    protected String createTraceFile(List<String> lines) throws IOException {
        Path traceFile = tempDir.resolve("trace_" + System.nanoTime() + ".txt");
        Files.write(traceFile, lines);
        logDebug("Trace file generated at: {}", traceFile.toAbsolutePath());
        return traceFile.toAbsolutePath().toString();
    }
}
