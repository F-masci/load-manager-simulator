package it.uniroma2.pmcsn.model.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the collection of Web Servers.
 * Handles dynamic capacity scaling (scaleUp and scaleDown) and deallocation draining.
 */
public class WebServerCluster {
    private static final Logger logger = LoggerFactory.getLogger(WebServerCluster.class);

    private final List<WebServer> activeServers;
    private final List<WebServer> drainingServers;
    private final List<WebServer> allServers; // Tracks all servers ever created for reporting

    private final int minServers;
    private final int maxServers;
    private final int webServerCapacity;

    private int scaleUpCount = 0;
    private int scaleDownCount = 0;

    public WebServerCluster(int minServers, int maxServers, int webServerCapacity) {
        this.minServers = minServers;
        this.maxServers = maxServers;
        this.webServerCapacity = webServerCapacity;
        this.activeServers = new ArrayList<>();
        this.drainingServers = new ArrayList<>();
        this.allServers = new ArrayList<>();

        for (int i = 1; i <= minServers; i++) {
            WebServer ws = new WebServer(i, webServerCapacity);
            activeServers.add(ws);
            allServers.add(ws);
        }
    }

    public WebServerCluster(int minServers, int maxServers, int webServerCapacity, List<WebServer> initialServers) {
        this.minServers = minServers;
        this.maxServers = maxServers;
        this.webServerCapacity = webServerCapacity;
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

    public int getWebServerCapacity() {
        return webServerCapacity;
    }

    /**
     * Increases the number of active Web Servers by 1, if below maxServers.
     */
    public boolean scaleUp(double clock) {
        if (activeServers.size() < maxServers) {
            int nextId = allServers.size() + 1;
            WebServer ws = new WebServer(nextId, webServerCapacity);
            ws.setLastEventTime(clock);
            activeServers.add(ws);
            allServers.add(ws);
            scaleUpCount++;
            logger.info("[Horizontal Scaling] Scale In: WebServer #{} added at clock={} (Active servers={})", nextId, clock, activeServers.size());
            return true;
        }
        logger.debug("Scale In: Ignored (already at maximum servers={}) at clock={}", maxServers, clock);
        return false;
    }

    /**
     * Decreases the number of active Web Servers by 1, if above minServers.
     * The removed server enters a draining state if it has active jobs.
     */
    public boolean scaleDown(double clock) {
        if (activeServers.size() > minServers) {
            WebServer ws = activeServers.remove(activeServers.size() - 1);
            ws.updateStatistics(clock);
            scaleDownCount++;
            if (!ws.getActiveJobs().isEmpty()) {
                drainingServers.add(ws);
                logger.info("[Horizontal Scaling] Scale Out: WebServer #{} entered draining state at clock={} (Active servers={})", ws.getId(), clock, activeServers.size());
            } else {
                logger.info("[Horizontal Scaling] Scale Out: Deallocated idle WebServer #{} at clock={} (Active servers={})", ws.getId(), clock, activeServers.size());
            }
            return true;
        }
        logger.debug("Scale Out: Ignored (already at minimum servers={}) at clock={}", minServers, clock);
        return false;
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

    public int getScaleUpCount() {
        return scaleUpCount;
    }

    public int getScaleDownCount() {
        return scaleDownCount;
    }
}
