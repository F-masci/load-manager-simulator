package it.uniroma2.pmcsn.model.load.routing.webserver;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.lib.rng.Rngs;
import it.uniroma2.pmcsn.lib.rng.Rvgs;
import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;

import java.util.List;

/**
 * Routing strategy that selects the best of two randomly chosen servers.
 */
public class PowerOfTwoChoicesRoutingStrategy implements WebServerRoutingStrategy {
    private static final int STREAM = 255;

    protected final Rngs rngs;
    protected final Rvgs rvgs;
    protected final long seed;

    /**
     * Constructs a strategy using the default application seed.
     */
    public PowerOfTwoChoicesRoutingStrategy() {
        this(ApplicationConfig.SEED);
    }

    /**
     * Constructs a strategy using a specific seed.
     *
     * @param seed the seed for random number generation
     */
    public PowerOfTwoChoicesRoutingStrategy(long seed) {
        this.rngs = new Rngs();
        this.rvgs = new Rvgs(rngs);
        this.seed = seed;

        rngs.plantSeeds(seed);
    }

    /**
     * Selects two random servers and returns the one with the fewest active jobs.
     *
     * @param job the job to route
     * @param cluster the web server cluster
     * @return the selected web server
     */
    @Override
    public WebServer selectWebServer(Job job, WebServerCluster cluster) {
        List<WebServer> active = cluster.getActiveServers();
        if (active.isEmpty()) return null;
        if (active.size() == 1) return active.getFirst();

        rngs.selectStream(STREAM);

        int i1 = (int) rvgs.equilikely(0, active.size() - 1);
        int i2;

        do {
            i2 = (int) rvgs.equilikely(0, active.size() - 1);
        } while (i1 == i2);

        WebServer s1 = active.get(i1);
        WebServer s2 = active.get(i2);

        return (s1.getActiveJobs().size() <= s2.getActiveJobs().size()) ? s1 : s2;
    }
}

