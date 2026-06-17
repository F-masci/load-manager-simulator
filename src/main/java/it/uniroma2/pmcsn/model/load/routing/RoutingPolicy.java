package it.uniroma2.pmcsn.model.load.routing;

/**
 * Defines the available routing policies for distributing jobs among web servers.
 */
public enum RoutingPolicy {
    ROUND_ROBIN, LEAST_LOADED, DETERMINISTIC, RANDOM, POWER_OF_TWO;

    /**
     * Compares this policy with a string representation.
     *
     * @param other the string to compare with
     * @return true if the names match ignoring case
     */
    public boolean equals(String other) {
        return this.name().equalsIgnoreCase(other);
    }
}
