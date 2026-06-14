package it.uniroma2.pmcsn.model.event;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.server.Server;

/**
 * Represents an event in the next-event driven simulation.
 */
public class Event implements Comparable<Event> {
    private final double time;
    private final EventType type;
    private final Job job;
    private final Server server; // The server where a completion occurs, or null for arrival

    public Event(double time, EventType type, Job job, Server server) {
        this.time = time;
        this.type = type;
        this.job = job;
        this.server = server;
    }

    public double getTime() {
        return time;
    }

    public EventType getType() {
        return type;
    }

    public Job getJob() {
        return job;
    }

    public Server getServer() {
        return server;
    }

    @Override
    public int compareTo(Event other) {
        int timeCompare = Double.compare(this.time, other.time);
        if (timeCompare != 0) {
            return timeCompare;
        }
        // Tie-breaker: prioritize completion events over arrivals, and arrivals over scale checks
        if (this.type != other.type) {
            return getPriority(this.type) - getPriority(other.type);
        }
        if (this.job != null && other.job != null) {
            return Integer.compare(this.job.getId(), other.job.getId());
        }
        return 0;
    }

    private int getPriority(EventType type) {
        switch (type) {
            case COMPLETION:                return 1;
            case ARRIVAL:                   return 2;
            case SCALE_CHECK_HORIZONTAL:    return 3;
            case SCALE_CHECK_VERTICAL:      return 4;
            default:                        return 5;
        }
    }

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
