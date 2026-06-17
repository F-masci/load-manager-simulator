package it.uniroma2.pmcsn.model.event.source;

import it.uniroma2.pmcsn.model.Job;

/**
 * Interface defining a source for job arrivals and service demands.
 */
public interface EventSource {
    
    /**
     * Generates or retrieves the next job in the sequence.
     *
     * @param lastArrivalTime the arrival time of the previous job
     * @return the next job or null if the sequence has ended
     */
    Job getNextJob(double lastArrivalTime);

    /**
     * Resets the source to its initial state.
     */
    void reset();

    /**
     * Gets the seed used for generation if applicable.
     *
     * @return the current seed value
     */
    long getSeed();

    /**
     * Seeds the generator for reproducible sequences.
     *
     * @param seed the seed value to set
     */
    void plantSeeds(long seed);
}
