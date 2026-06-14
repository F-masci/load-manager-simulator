package it.uniroma2.pmcsn.model.load;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.load.routing.Router;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.HorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.VerticalScaler;
import it.uniroma2.pmcsn.model.server.Server;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;

/**
 * Coordinates routing and horizontal/vertical autoscaling activities by wrapping a HorizontalScaler,
 * a VerticalScaler, and a Router.
 */
public class LoadManager {
    private final HorizontalScaler horizontalScaler;
    private final VerticalScaler verticalScaler;
    private final Router router;

    public LoadManager(HorizontalScaler horizontalScaler, VerticalScaler verticalScaler, Router router) {
        this.horizontalScaler = horizontalScaler;
        this.verticalScaler = verticalScaler;
        this.router = router;
    }

    /**
     * Delegates job routing decisions to the Router (using the Template Method).
     */
    public Server routeJob(Job job, WebServerCluster cluster, SpikeServer spikeServer) {
        return router.route(job, cluster, spikeServer);
    }

    /**
     * Returns true if any scaling action occurred.
     */
    public boolean evaluateScaling(double clock, WebServerCluster cluster) {
        return horizontalScaler.evaluateScaling(clock, cluster);
    }

    /**
     * Returns true if any scaling action occurred.
     */
    public boolean evaluateScaling(double clock, SpikeServer spikeServer) {
        return verticalScaler.evaluateScaling(clock, spikeServer);
    }

    public HorizontalScaler getHorizontalScaler() {
        return horizontalScaler;
    }

    public VerticalScaler getVerticalScaler() {
        return verticalScaler;
    }

    public Router getRouter() {
        return router;
    }
}
