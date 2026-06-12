package it.uniroma2.pmcsn.configs;

/**
 * Enumeration of workload generation types in the simulation.
 */
public enum WorkloadType {
    DISTRIBUTION,
    HYPEREXPONENTIAL,
    TRACE;

    /**
     * Overloaded equals method to allow direct comparison with string representations.
     */
    public boolean equals(String other) {
        return other != null && this.name().equalsIgnoreCase(other);
    }
}
