package it.uniroma2.pmcsn.model.event.source;

import it.uniroma2.pmcsn.lib.rng.Rngs;
import it.uniroma2.pmcsn.lib.rng.Rvgs;

/**
 * Abstract base for distribution-driven event sources using random variate generation.
 */
public abstract class DistributionEventSource implements EventSource {
    protected final Rngs rngs;
    protected final Rvgs rvgs;
    protected final long seed;
    protected int jobCounter = 0;

    /**
     * Initializes the distribution source with a seed and sets up generators.
     *
     * @param seed the seed for random number generation
     */
    protected DistributionEventSource(long seed) {
        this.rngs = new Rngs();
        this.rvgs = new Rvgs(rngs);
        this.seed = seed;
        reset();
    }

    /**
     * Resets the job counter and restores generators to their initial state.
     */
    @Override
    public void reset() {
        jobCounter = 0;
        rngs.plantSeeds(seed);
    }

    /**
     * Gets the current seed of the underlying random number generator.
     *
     * @return the current seed
     */
    @Override
    public long getSeed() {
        return rngs.getSeed();
    }

    /**
     * Re-seeds the random number generator.
     *
     * @param seed the new seed value
     */
    @Override
    public void plantSeeds(long seed) {
        rngs.plantSeeds(seed);
    }
}
