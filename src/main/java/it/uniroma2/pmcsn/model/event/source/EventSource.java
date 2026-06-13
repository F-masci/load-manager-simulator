package it.uniroma2.pmcsn.model.event.source;

import it.uniroma2.pmcsn.model.Job;

/**
 * Source of job arrivals and their service times for the simulation.
 */
public interface EventSource {
    
    /**
     * Generates or reads the next job.
     *
     * @param lastArrivalTime The arrival time of the previous job
     * @return The next job, or null if no more jobs are available
     */
    Job getNextJob(double lastArrivalTime);

    /**
     * Resets the event source state.
     */
    void reset();

    /**
     * Gets the seed used for random number generation, if applicable.
     *
     * @return The seed value
     */
    long getSeed();

    /**
     * Set the seed used for random number generation, if applicable.
     *
     * @param seed The seed value
     */
    void plantSeeds(long seed);
}
