package it.uniroma2.pmcsn.model.load;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.load.routing.Router;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.HorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.VerticalScaler;
import it.uniroma2.pmcsn.model.server.Server;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;

/**
 * Coordinates routing and autoscaling activities by managing a HorizontalScaler, a VerticalScaler, and a Router.
 */
public class LoadManager {
    private final HorizontalScaler horizontalScaler;
    private final VerticalScaler verticalScaler;
    private final Router router;

    /**
     * Constructs a LoadManager with specified scaling and routing components.
     *
     * @param horizontalScaler the component for horizontal scaling
     * @param verticalScaler the component for vertical scaling
     * @param router the component for job routing
     */
    public LoadManager(HorizontalScaler horizontalScaler, VerticalScaler verticalScaler, Router router) {
        this.horizontalScaler = horizontalScaler;
        this.verticalScaler = verticalScaler;
        this.router = router;
    }

    /**
     * Routes a job to an appropriate server using the internal router.
     *
     * @param job the job to be routed
     * @param cluster the cluster of web servers
     * @param spikeServer the dedicated spike server
     * @return the selected server for the job
     */
    public Server routeJob(Job job, WebServerCluster cluster, SpikeServer spikeServer) {
        return router.route(job, cluster, spikeServer);
    }

    /**
     * Evaluates and performs horizontal scaling actions for the cluster.
     *
     * @param clock the current simulation time
     * @param cluster the cluster to evaluate for scaling
     * @return true if a horizontal scaling action occurred
     */
    public boolean evaluateScaling(double clock, WebServerCluster cluster) {
        return horizontalScaler.evaluateScaling(clock, cluster);
    }

    /**
     * Evaluates and performs vertical scaling actions for the spike server.
     *
     * @param clock the current simulation time
     * @param spikeServer the spike server to evaluate for scaling
     * @return true if a vertical scaling action occurred
     */
    public boolean evaluateScaling(double clock, SpikeServer spikeServer) {
        return verticalScaler.evaluateScaling(clock, spikeServer);
    }

    /**
     * Gets the horizontal scaler component.
     *
     * @return the horizontal scaler
     */
    public HorizontalScaler getHorizontalScaler() {
        return horizontalScaler;
    }

    /**
     * Gets the vertical scaler component.
     *
     * @return the vertical scaler
     */
    public VerticalScaler getVerticalScaler() {
        return verticalScaler;
    }

    /**
     * Gets the router component.
     *
     * @return the router
     */
    public Router getRouter() {
        return router;
    }
}
