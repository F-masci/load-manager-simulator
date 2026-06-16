package it.uniroma2.pmcsn.configs;

/**
 * Enumeration of workload generation types in the simulation.
 */
public enum WorkloadType {
    EXPONENTIAL,
    HYPEREXPONENTIAL,
    TRACE;

    public boolean isDistributionWorkload() {
        return this == EXPONENTIAL || this == HYPEREXPONENTIAL;
    }
}
