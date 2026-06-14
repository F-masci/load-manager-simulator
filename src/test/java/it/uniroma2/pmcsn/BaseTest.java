package it.uniroma2.pmcsn;

import it.uniroma2.pmcsn.utils.LogFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.File;

/**
 * Base class for all tests, providing consistent logging and common utilities.
 */
public abstract class BaseTest {
    protected final LogFactory.ModuleLogger logger = LogFactory.getLogger(getClass());
    protected static final Marker TEST_MARKER = MarkerFactory.getMarker("TEST");

    /**
     * Logs a high-level test step using the TEST marker.
     * RANK: Between INFO and WARN (conceptually).
     *
     * @param message The message to log.
     * @param args Optional arguments for the message.
     */
    protected void logTestStep(String message, Object... args) {
        logger.info(TEST_MARKER, message, args);
    }

    /**
     * Logs technical details at DEBUG level.
     *
     * @param message The message to log.
     * @param args Optional arguments for the message.
     */
    protected void logDebug(String message, Object... args) {
        logger.debug(message, args);
    }

    /**
     * Ensures that the specified directories exist, creating them if necessary.
     * Useful for setup phases to prepare output folders.
     *
     * @param paths Paths to ensure existence for.
     */
    protected static void ensureDirectories(String... paths) {
        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists() && dir.mkdirs()) {
                // Static access to LoggerFactory since instance loggers are not available
                org.slf4j.LoggerFactory.getLogger(BaseTest.class).debug("Created directory: {}", path);
            }
        }
    }
}
