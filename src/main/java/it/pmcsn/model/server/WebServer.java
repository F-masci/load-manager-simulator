package it.pmcsn.model.server;

/**
 * Represents a Web Server (Layer 1) using Processor Sharing scheduling.
 */
public class WebServer extends Server {

    public WebServer(int id, int capacity) {
        super(id, capacity, 1.0);
    }

    /**
     * Spike Indicator (SI): number of requests currently in execution on this server.
     */
    public int getSpikeIndicator() {
        return activeJobs.size();
    }
}
