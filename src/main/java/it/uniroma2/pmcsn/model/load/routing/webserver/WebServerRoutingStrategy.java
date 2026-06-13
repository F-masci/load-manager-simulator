package it.uniroma2.pmcsn.model.load.routing.webserver;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;

/**
 * Strategy interface for selecting a Web Server from the cluster.
 */
public interface WebServerRoutingStrategy {
    /**
     * Selects a target Web Server from the cluster.
     *
     * @param job     the job to route
     * @param cluster the cluster of web servers
     * @return the selected Web Server
     */
    WebServer selectWebServer(Job job, WebServerCluster cluster);
}
