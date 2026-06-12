package it.uniroma2.pmcsn.model.event.source;

import it.uniroma2.pmcsn.lib.rng.Rngs;
import it.uniroma2.pmcsn.lib.rng.Rvgs;

/**
 * Abstract base class for distribution-driven event sources.
 * Encapsulates the multi-stream Lehmer LCG generator and random variate models.
 */
public abstract class DistributionEventSource implements EventSource {
    protected final Rngs rngs;
    protected final Rvgs rvgs;
    protected final long seed;
    protected int jobCounter = 0;

    protected DistributionEventSource(long seed) {
        this.rngs = new Rngs();
        this.rvgs = new Rvgs(rngs);
        this.seed = seed;
        reset();
    }

    @Override
    public void reset() {
        jobCounter = 0;
        rngs.plantSeeds(seed);
    }
}
