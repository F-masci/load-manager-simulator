package it.uniroma2.pmcsn.configs;

/**
 * Enumeration of workload generation types in the simulation.
 */
public enum WorkloadType {
    /** Exponential distribution. */
    EXPONENTIAL,
    /** Hyperexponential distribution. */
    HYPEREXPONENTIAL,
    /** Trace-driven workload. */
    TRACE;

    /**
     * Checks if the workload type is based on a probability distribution.
     *
     * @return true if distribution-based, false otherwise
     */
    public boolean isDistributionWorkload() {
        return this == EXPONENTIAL || this == HYPEREXPONENTIAL;
    }
}
