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
     * Creates a logger with a default marker.
     *
     * @param clazz the class for the logger
     * @return the module logger
     */
    public static ModuleLogger getLogger(Class<?> clazz) {
        return new ModuleLogger(LoggerFactory.getLogger(clazz), DEFAULT_MARKER);
    }

    /**
     * Creates a logger for a specific module.
     *
     * @param clazz the class for the logger
     * @param moduleName the name of the module
     * @return the module logger
     */
    public static ModuleLogger getLogger(Class<?> clazz, String moduleName) {
        return new ModuleLogger(LoggerFactory.getLogger(clazz), MarkerFactory.getMarker(moduleName.toUpperCase()));
    }

    /**
     * Logger wrapper that automatically attaches markers to log entries.
     */
    public static class ModuleLogger {
        private final Logger delegate;
        private final Marker marker;

        /**
         * Constructs a module logger.
         *
         * @param delegate the slf4j logger
         * @param marker the marker to attach
         */
        public ModuleLogger(Logger delegate, Marker marker) {
            this.delegate = delegate;
            this.marker = marker;
        }

        /**
         * Logs an info message.
         *
         * @param msg the message
         * @param args the arguments
         */
        public void info(String msg, Object... args) { delegate.info(marker, msg, args); }

        /**
         * Logs a debug message.
         *
         * @param msg the message
         * @param args the arguments
         */
        public void debug(String msg, Object... args) { delegate.debug(marker, msg, args); }

        /**
         * Logs a warning message.
         *
         * @param msg the message
         * @param args the arguments
         */
        public void warn(String msg, Object... args) { delegate.warn(marker, msg, args); }

        /**
         * Logs an error message.
         *
         * @param msg the message
         * @param args the arguments
         */
        public void error(String msg, Object... args) { delegate.error(marker, msg, args); }

        /**
         * Logs a trace message.
         *
         * @param msg the message
         * @param args the arguments
         */
        public void trace(String msg, Object... args) { delegate.trace(marker, msg, args); }
        
        /**
         * Logs an info message with a custom marker.
         *
         * @param m the custom marker
         * @param msg the message
         * @param args the arguments
         */
        public void info(Marker m, String msg, Object... args) { delegate.info(m, msg, args); }
    }
}
