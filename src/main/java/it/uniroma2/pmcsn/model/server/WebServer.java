package it.uniroma2.pmcsn.model.server;

/**
 * Represents a Web Server (Layer 1) using Processor Sharing scheduling.
 */
public class WebServer extends Server {

    /**
     * Constructs a WebServer with a given ID and unit speed multiplier.
     *
     * @param id the unique identifier of the server
     */
    public WebServer(int id) {
        super(id, 1.0);
    }

    /**
     * Returns the Spike Indicator (SI), defined as the number of requests currently in execution.
     *
     * @return the number of active jobs
     */
    public int getSpikeIndicator() {
        return activeJobs.size();
    }
}
