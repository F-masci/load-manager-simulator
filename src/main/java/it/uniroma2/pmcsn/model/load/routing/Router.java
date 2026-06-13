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
 * Concrete Router class that coordinates the routing using the configured
 * WebServerRoutingStrategy and SpikeServerRoutingStrategy.
 */
public class Router {
    protected final int siMax;
    protected WebServerRoutingStrategy webServerRoutingStrategy;
    protected SpikeServerRoutingStrategy spikeServerRoutingStrategy;

    /**
     * Main constructor accepting both web server selection and spike server routing strategies.
     *
     * @param siMax                      the threshold configuration for Spike Indicator
     * @param webServerRoutingStrategy   the strategy for selecting Web Servers
     * @param spikeServerRoutingStrategy the strategy for deciding to route to the Spike Server
     */
    public Router(int siMax, WebServerRoutingStrategy webServerRoutingStrategy, SpikeServerRoutingStrategy spikeServerRoutingStrategy) {
        this.siMax = siMax;
        this.webServerRoutingStrategy = webServerRoutingStrategy;
        this.spikeServerRoutingStrategy = spikeServerRoutingStrategy;
    }

    /**
     * Fallback/Convenience constructor that defaults to ThresholdSpikeServerRoutingStrategy.
     *
     * @param siMax                    the threshold configuration for Spike Indicator
     * @param webServerRoutingStrategy the strategy for selecting Web Servers
     */
    public Router(int siMax, WebServerRoutingStrategy webServerRoutingStrategy) {
        this(siMax, webServerRoutingStrategy, new ThresholdSpikeServerRoutingStrategy());
    }

    /**
     * Routes a job to either a Web Server or the Spike Server based on the active strategies.
     *
     * @param job         the job to route
     * @param cluster     the web server cluster
     * @param spikeServer the spike server
     * @return the selected Server
     */
    public final Server route(Job job, WebServerCluster cluster, SpikeServer spikeServer) {
        WebServer targetServer = webServerRoutingStrategy.selectWebServer(job, cluster);
        if (spikeServerRoutingStrategy.shouldRouteToSpike(targetServer, siMax)) {
            return spikeServer;
        }
        return targetServer;
    }

    public int getSiMax() {
        return siMax;
    }

    public WebServerRoutingStrategy getWebServerRoutingStrategy() {
        return webServerRoutingStrategy;
    }

    public void setWebServerRoutingStrategy(WebServerRoutingStrategy strategy) {
        this.webServerRoutingStrategy = strategy;
    }

    public SpikeServerRoutingStrategy getSpikeServerRoutingStrategy() {
        return spikeServerRoutingStrategy;
    }

    public void setSpikeServerRoutingStrategy(SpikeServerRoutingStrategy strategy) {
        this.spikeServerRoutingStrategy = strategy;
    }
}
