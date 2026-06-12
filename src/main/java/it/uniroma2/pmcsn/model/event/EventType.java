package it.uniroma2.pmcsn.model.event;

/**
 * Enumeration of event types in the simulation.
 */
public enum EventType {
    ARRIVAL,
    COMPLETION,
    SCALE_CHECK;

    /**
     * Overloaded equals method to allow direct comparison with string representations.
     */
    public boolean equals(String other) {
        return other != null && this.name().equalsIgnoreCase(other);
    }
}
