package it.pmcsn.model.load;

import it.pmcsn.model.Job;
import it.pmcsn.model.server.Server;
import it.pmcsn.model.server.SpikeServer;
import it.pmcsn.model.load.scaler.horizontal.HorizontalScaler;
import it.pmcsn.model.load.scaler.vertical.VerticalScaler;
import it.pmcsn.model.load.routing.Router;
import it.pmcsn.model.server.WebServerCluster;

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
     * Triggers horizontal and vertical autoscaling checks.
     * Returns true if any scaling action occurred.
     */
    public boolean evaluateScaling(double clock, WebServerCluster cluster, SpikeServer spikeServer) {
        boolean hScaled = horizontalScaler.evaluateScaling(clock, cluster);
        boolean vScaled = verticalScaler.evaluateScaling(clock, spikeServer);
        return hScaled || vScaled;
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
