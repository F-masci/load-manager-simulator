package it.uniroma2.pmcsn.configs;

/**
 * Supported types of simulation data to be captured and logged.
 */
public enum LoggingDataType {
    /**
     * Captures load metrics for Web Server and Spike Server comparison.
     */
    LOAD_COMPARISON,
    
    /**
     * Basic system-wide metrics (Response Time, JIS, etc.).
     */
    SYSTEM_METRICS,
    
    /**
     * Scaling metrics and thresholds for horizontal and vertical scalers.
     */
    SCALING_METRICS
}
