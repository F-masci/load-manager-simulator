package it.uniroma2.pmcsn.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Custom logging factory to handle modular logging with SLF4J Markers.
 * Allows defining a logger associated with a specific module (e.g., SCALER, SERVER).
 * Uses 'SIM' as the default marker if none is specified.
 */
public class LogFactory {
    private static final Marker DEFAULT_MARKER = MarkerFactory.getMarker("SIM");

    /**
     * Creates a ModuleLogger which automatically attaches a marker to every log call.
     */
    public static ModuleLogger getLogger(Class<?> clazz) {
        return new ModuleLogger(LoggerFactory.getLogger(clazz), DEFAULT_MARKER);
    }

    /**
     * Creates a ModuleLogger associated with a specific module name.
     */
    public static ModuleLogger getLogger(Class<?> clazz, String moduleName) {
        return new ModuleLogger(LoggerFactory.getLogger(clazz), MarkerFactory.getMarker(moduleName.toUpperCase()));
    }

    /**
     * Wrapper for SLF4J Logger that injects a Marker into all logging methods.
     */
    public static class ModuleLogger {
        private final Logger delegate;
        private final Marker marker;

        public ModuleLogger(Logger delegate, Marker marker) {
            this.delegate = delegate;
            this.marker = marker;
        }

        public void info(String msg, Object... args) { delegate.info(marker, msg, args); }
        public void debug(String msg, Object... args) { delegate.debug(marker, msg, args); }
        public void warn(String msg, Object... args) { delegate.warn(marker, msg, args); }
        public void error(String msg, Object... args) { delegate.error(marker, msg, args); }
        public void trace(String msg, Object... args) { delegate.trace(marker, msg, args); }
        
        // Special case for passing a different marker (e.g. TEST in tests)
        public void info(Marker m, String msg, Object... args) { delegate.info(m, msg, args); }
    }
}
