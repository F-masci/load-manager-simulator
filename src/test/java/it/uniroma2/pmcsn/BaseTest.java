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
     * Use this for tracing significant logical checkpoints in a test.
     *
     * @param message The message template to log.
     * @param args    Optional arguments for variable substitution.
     */
    protected void logTestStep(String message, Object... args) {
        logger.info(TEST_MARKER, message, args);
    }

    /**
     * Logs technical details or intermediate values at the DEBUG level.
     *
     * @param message The message template to log.
     * @param args    Optional arguments for variable substitution.
     */
    protected void logDebug(String message, Object... args) {
        logger.debug(message, args);
    }

    /**
     * Ensures that the specified directories exist on the filesystem.
     * Creates any missing parent directories as well.
     *
     * @param paths Varargs list of directory paths to verify or create.
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
