package it.uniroma2.pmcsn.model.load.routing.webserver;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.lib.rng.Rngs;
import it.uniroma2.pmcsn.lib.rng.Rvgs;
import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;

import java.util.List;
import java.util.Random;

/**
 * Randomly selects a Web Server from the active ones.
 */
public class RandomRoutingStrategy implements WebServerRoutingStrategy {
    private static final int STREAM = 255;

    protected final Rngs rngs;
    protected final Rvgs rvgs;
    protected final long seed;

    public RandomRoutingStrategy() {
        this(ApplicationConfig.SEED);
    }

    public RandomRoutingStrategy(long seed) {
        this.rngs = new Rngs();
        this.rvgs = new Rvgs(rngs);
        this.seed = seed;

        rngs.plantSeeds(seed);
    }

    @Override
    public WebServer selectWebServer(Job job, WebServerCluster cluster) {
        List<WebServer> active = cluster.getActiveServers();
        if (active.isEmpty()) return null;
        if (active.size() == 1) return active.getFirst();

        rngs.selectStream(STREAM);
        int randomIndex = (int) rvgs.equilikely(0, active.size() - 1);

        return active.get(randomIndex);
    }
}
