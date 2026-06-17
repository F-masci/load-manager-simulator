package it.uniroma2.pmcsn.model.load.routing;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.load.routing.spike.SpikeServerRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.spike.ThresholdSpikeServerRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.WebServerRoutingStrategy;
import it.uniroma2.pmcsn.model.server.Server;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;

/**
 * Coordinates job routing using web server and spike server routing strategies.
 */
public class Router {
    protected final int siMax;
    protected WebServerRoutingStrategy webServerRoutingStrategy;
    protected SpikeServerRoutingStrategy spikeServerRoutingStrategy;

    /**
     * Constructs a Router with specific strategies for web and spike server routing.
     *
     * @param siMax the threshold configuration for the spike indicator
     * @param webServerRoutingStrategy the strategy for selecting web servers
     * @param spikeServerRoutingStrategy the strategy for deciding when to route to the spike server
     */
    public Router(int siMax, WebServerRoutingStrategy webServerRoutingStrategy, SpikeServerRoutingStrategy spikeServerRoutingStrategy) {
        this.siMax = siMax;
        this.webServerRoutingStrategy = webServerRoutingStrategy;
        this.spikeServerRoutingStrategy = spikeServerRoutingStrategy;
    }

    /**
     * Constructs a Router with a default threshold-based spike routing strategy.
     *
     * @param siMax the threshold configuration for the spike indicator
     * @param webServerRoutingStrategy the strategy for selecting web servers
     */
    public Router(int siMax, WebServerRoutingStrategy webServerRoutingStrategy) {
        this(siMax, webServerRoutingStrategy, new ThresholdSpikeServerRoutingStrategy());
    }

    /**
     * Routes a job to either a web server or the spike server based on the active strategies.
     *
     * @param job the job to route
     * @param cluster the web server cluster
     * @param spikeServer the spike server
     * @return the selected server for the job
     */
    public final Server route(Job job, WebServerCluster cluster, SpikeServer spikeServer) {
        WebServer targetServer = webServerRoutingStrategy.selectWebServer(job, cluster);
        if (spikeServerRoutingStrategy.shouldRouteToSpike(targetServer, siMax)) {
            return spikeServer;
        }
        return targetServer;
    }

    /**
     * Gets the spike indicator maximum threshold.
     *
     * @return the maximum threshold
     */
    public int getSiMax() {
        return siMax;
    }

    /**
     * Gets the current web server routing strategy.
     *
     * @return the web server routing strategy
     */
    public WebServerRoutingStrategy getWebServerRoutingStrategy() {
        return webServerRoutingStrategy;
    }

    /**
     * Sets a new web server routing strategy.
     *
     * @param strategy the new strategy to use
     */
    public void setWebServerRoutingStrategy(WebServerRoutingStrategy strategy) {
        this.webServerRoutingStrategy = strategy;
    }

    /**
     * Gets the current spike server routing strategy.
     *
     * @return the spike server routing strategy
     */
    public SpikeServerRoutingStrategy getSpikeServerRoutingStrategy() {
        return spikeServerRoutingStrategy;
    }

    /**
     * Sets a new spike server routing strategy.
     *
     * @param strategy the new strategy to use
     */
    public void setSpikeServerRoutingStrategy(SpikeServerRoutingStrategy strategy) {
        this.spikeServerRoutingStrategy = strategy;
    }
}
