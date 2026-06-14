package it.uniroma2.pmcsn.model.server;

import it.uniroma2.pmcsn.utils.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the collection of Web Servers.
 * Handles dynamic capacity scaling (scaleOut and scaleIn) and deallocation draining.
 */
public class WebServerCluster {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(WebServerCluster.class, "SERVER");

    private final List<WebServer> activeServers;
    private final List<WebServer> drainingServers;
    private final List<WebServer> allServers; // Tracks all servers ever created for reporting

    private final int minServers;
    private final int maxServers;

    private int scaleOutCount = 0;
    private int scaleInCount = 0;

    public WebServerCluster(int minServers, int maxServers) {
        this.minServers = minServers;
        this.maxServers = maxServers;
        this.activeServers = new ArrayList<>();
        this.drainingServers = new ArrayList<>();
        this.allServers = new ArrayList<>();

        for (int i = 1; i <= minServers; i++) {
            WebServer ws = new WebServer(i);
            activeServers.add(ws);
            allServers.add(ws);
        }
    }

    public WebServerCluster(int minServers, int maxServers, List<WebServer> initialServers) {
        this.minServers = minServers;
        this.maxServers = maxServers;
        this.activeServers = new ArrayList<>(initialServers);
        this.drainingServers = new ArrayList<>();
        this.allServers = new ArrayList<>(initialServers);
    }

    public List<WebServer> getActiveServers() {
        return activeServers;
    }

    public List<WebServer> getDrainingServers() {
        return drainingServers;
    }

    public List<WebServer> getAllServers() {
        return allServers;
    }

    public int getMinServers() {
        return minServers;
    }

    public int getMaxServers() {
        return maxServers;
    }

    /**
     * Increases the number of active Web Servers by 1, if below maxServers.
     */
    public boolean scaleOut(double clock) {
        if (activeServers.size() < maxServers) {
            int nextId = allServers.size() + 1;
            WebServer ws = new WebServer(nextId);
            activeServers.add(ws);
            allServers.add(ws);
            scaleOutCount++;
            logger.info("Scale Out: WebServer #{} added at clock={} (Active servers={})", nextId, clock, activeServers.size());
            return true;
        }
        logger.debug("Scale Out: Ignored (already at maximum servers={}) at clock={}", maxServers, clock);
        return false;
    }

    /**
     * Decreases the number of active Web Servers by 1, if above minServers.
     * The removed server enters a draining state if it has active jobs.
     */
    public boolean scaleIn(double clock) {
        if (activeServers.size() > minServers) {
            WebServer ws = activeServers.remove(activeServers.size() - 1);
            ws.updateStatistics(clock);
            scaleInCount++;
            if (!ws.getActiveJobs().isEmpty()) {
                drainingServers.add(ws);
                logger.info("Scale In: WebServer #{} entered draining state at clock={} (Active servers={})", ws.getId(), clock, activeServers.size());
            } else {
                logger.info("Scale In: Deallocated idle WebServer #{} at clock={} (Active servers={})", ws.getId(), clock, activeServers.size());
            }
            return true;
        }
        logger.debug("Scale In: Ignored (already at minimum servers={}) at clock={}", minServers, clock);
        return false;
    }

    /**
     * Updates statistics for all active and draining servers.
     */
    public void updateStatistics(double clock) {
        for (WebServer ws : allServers) {
            ws.updateStatistics(clock);
        }
    }

    /**
     * Updates statistics and processes jobs for all active and draining servers.
     */
    public void processActiveJobs(double elapsed, double nextTime) {
        for (WebServer ws : activeServers) {
            ws.updateStatistics(nextTime);
            ws.processJobs(elapsed);
        }
        for (WebServer ws : drainingServers) {
            ws.updateStatistics(nextTime);
            ws.processJobs(elapsed);
        }
        // Clean up empty draining servers
        drainingServers.removeIf(ws -> ws.getActiveJobs().isEmpty());
    }

    /**
     * Updates statistics for all servers at the end of the simulation.
     */
    public void finalizeStatistics(double clock) {
        for (WebServer ws : allServers) {
            ws.updateStatistics(clock);
        }
    }

    /**
     * Resets statistics for all servers in the cluster and scaling counts.
     */
    public void resetStatistics(double clock) {
        for (WebServer ws : allServers) {
            ws.resetStatistics(clock);
        }
        scaleOutCount = 0;
        scaleInCount = 0;
    }

    public int getScaleOutCount() {
        return scaleOutCount;
    }

    public int getScaleInCount() {
        return scaleInCount;
    }
}
