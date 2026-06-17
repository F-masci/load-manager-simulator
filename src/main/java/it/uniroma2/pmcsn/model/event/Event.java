package it.uniroma2.pmcsn.model.event;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.server.Server;

/**
 * Represents an event in the next-event driven simulation with priority handling.
 */
public class Event implements Comparable<Event> {
    private final double time;
    private final EventType type;
    private final Job job;
    private final Server server; // The server where a completion occurs, or null for arrival

    /**
     * Initializes an event with time, type, associated job, and server.
     *
     * @param time the scheduled time of the event
     * @param type the type of the event
     * @param job the job associated with the event
     * @param server the server associated with the event
     */
    public Event(double time, EventType type, Job job, Server server) {
        this.time = time;
        this.type = type;
        this.job = job;
        this.server = server;
    }

    /**
     * Gets the scheduled time of the event.
     *
     * @return the event time
     */
    public double getTime() {
        return time;
    }

    /**
     * Gets the type of the event.
     *
     * @return the event type
     */
    public EventType getType() {
        return type;
    }

    /**
     * Gets the associated job.
     *
     * @return the job or null if not applicable
     */
    public Job getJob() {
        return job;
    }

    /**
     * Gets the associated server.
     *
     * @return the server or null if not applicable
     */
    public Server getServer() {
        return server;
    }

    /**
     * Compares this event with another for chronological ordering and priority tie-breaking.
     *
     * @param other the other event to compare to
     * @return comparison result
     */
    @Override
    public int compareTo(Event other) {
        int timeCompare = Double.compare(this.time, other.time);
        if (timeCompare != 0) {
            return timeCompare;
        }
        // Prioritize completion events over arrivals, and arrivals over scale checks
        if (this.type != other.type) {
            return getPriority(this.type) - getPriority(other.type);
        }
        if (this.job != null && other.job != null) {
            return Integer.compare(this.job.getId(), other.job.getId());
        }
        return 0;
    }

    /**
     * Retrieves the numerical priority for an event type to resolve scheduling ties.
     *
     * @param type the event type
     * @return priority value where lower means higher precedence
     */
    private int getPriority(EventType type) {
        switch (type) {
            case COMPLETION:                return 1;
            case ARRIVAL:                   return 2;
            case SCALE_CHECK_HORIZONTAL:    return 3;
            case SCALE_CHECK_VERTICAL:      return 4;
            default:                        return 5;
        }
    }

    /**
     * Returns a string representation of the event state.
     *
     * @return formatted string
     */
    @Override
    public String toString() {
        return "Event{" +
                "time=" + time +
                ", type=" + type +
                ", job=" + (job != null ? job.getId() : "null") +
                ", server=" + (server != null ? server.getId() : "null") +
                '}';
    }
}
