package it.uniroma2.pmcsn.model.load.routing;

public enum RoutingPolicy {
    ROUND_ROBIN, LEAST_LOADED;

    /**
     * Overloaded equals method to allow direct comparison with string representations.
     */
    public boolean equals(String other) {
        return this.name().equalsIgnoreCase(other);
    }
}
