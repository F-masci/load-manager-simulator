package it.uniroma2.pmcsn.model.event;

/**
 * Enumeration of event types supported in the simulation.
 */
public enum EventType {
    ARRIVAL,
    COMPLETION,
    SCALE_CHECK_HORIZONTAL,
    SCALE_CHECK_VERTICAL;

    /**
     * Compares this event type with a string representation case-insensitively.
     *
     * @param other the string to compare with
     * @return true if names match
     */
    public boolean equals(String other) {
        return other != null && this.name().equalsIgnoreCase(other);
    }
}
