package it.uniroma2.pmcsn.model.server;

import it.uniroma2.pmcsn.lib.statistics.TimedWelford;
import it.uniroma2.pmcsn.utils.LogFactory;
import it.uniroma2.pmcsn.utils.LogFactory.ModuleLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the collection of Web Servers.
 * Handles dynamic capacity scaling (scaleOut and scaleIn) and deallocation draining.
 */
public class WebServerCluster {
    private static final ModuleLogger logger = LogFactory.getLogger(WebServerCluster.class, "SERVER");

    private final List<WebServer> activeServers;
    private final List<WebServer> drainingServers;
    private final List<WebServer> allServers; // Tracks all servers ever created for reporting

    private final int minServers;
    private final int maxServers;

    private int scaleOutCount = 0;
    private int scaleInCount = 0;
    
    private final TimedWelford activeServersStat = new TimedWelford();

    /**
     * Constructs a WebServerCluster with a specified minimum and maximum number of servers.
     *
     * @param minServers the minimum number of active servers
     * @param maxServers the maximum number of active servers
     */
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
        activeServersStat.updateToTime(0.0, activeServers.size());
    }

    /**
     * Constructs a WebServerCluster with specified limits and an initial list of servers.
     *
     * @param minServers the minimum number of active servers
     * @param maxServers the maximum number of active servers
     * @param initialServers the list of initially active servers
     */
    public WebServerCluster(int minServers, int maxServers, List<WebServer> initialServers) {
        this.minServers = minServers;
        this.maxServers = maxServers;
        this.activeServers = new ArrayList<>(initialServers);
        this.drainingServers = new ArrayList<>();
        this.allServers = new ArrayList<>(initialServers);
        activeServersStat.updateToTime(0.0, activeServers.size());
    }

    /**
     * Returns the current number of active servers.
     *
     * @return the active servers count
     */
    public int getActiveServersCount() {
        return activeServers.size();
    }

    /**
     * Returns the current number of draining servers.
     *
     * @return the draining servers count
     */
    public int getDrainingServersCount() {
        return drainingServers.size();
    }

    /**
     * Returns the list of currently active servers.
     *
     * @return the list of active servers
     */
    public List<WebServer> getActiveServers() {
        return activeServers;
    }

    /**
     * Returns the list of servers currently in the draining state.
     *
     * @return the list of draining servers
     */
    public List<WebServer> getDrainingServers() {
        return drainingServers;
    }

    /**
     * Returns the list of all servers created during the simulation.
     *
     * @return the list of all servers
     */
    public List<WebServer> getAllServers() {
        return allServers;
    }

    /**
     * Returns the minimum number of servers.
     *
     * @return the minimum servers limit
     */
    public int getMinServers() {
        return minServers;
    }

    /**
     * Returns the maximum number of servers.
     *
     * @return the maximum servers limit
     */
    public int getMaxServers() {
        return maxServers;
    }

    /**
     * Increases the number of active Web Servers by 1, if below maxServers.
     *
     * @param clock current simulation time
     * @return true if a server was added, false otherwise
     */
    public boolean scaleOut(double clock) {
        if (activeServers.size() < maxServers) {
            int nextId = allServers.size() + 1;
            WebServer ws = new WebServer(nextId);
            activeServers.add(ws);
            allServers.add(ws);
            scaleOutCount++;
            activeServersStat.updateToTime(clock, activeServers.size());
            logger.debug("Scale Out: WebServer #{} added at clock={} (Active servers={})", nextId, clock, activeServers.size());
            return true;
        }
        logger.debug("Scale Out: Ignored (already at maximum servers={}) at clock={}", maxServers, clock);
        return false;
    }

    /**
     * Decreases the number of active Web Servers by 1, if above minServers.
     * The removed server enters a draining state if it has active jobs.
     *
     * @param clock current simulation time
     * @return true if a server was removed from active pool, false otherwise
     */
    public boolean scaleIn(double clock) {
        if (activeServers.size() > minServers) {
            WebServer ws = activeServers.remove(activeServers.size() - 1);
            ws.updateStatistics(clock);
            scaleInCount++;
            activeServersStat.updateToTime(clock, activeServers.size());
            if (!ws.getActiveJobs().isEmpty()) {
                drainingServers.add(ws);
                logger.debug("Scale In: WebServer #{} entered draining state at clock={} (Active servers={})", ws.getId(), clock, activeServers.size());
            } else {
                logger.debug("Scale In: Deallocated idle WebServer #{} at clock={} (Active servers={})", ws.getId(), clock, activeServers.size());
            }
            return true;
        }
        logger.debug("Scale In: Ignored (already at minimum servers={}) at clock={}", minServers, clock);
        return false;
    }

    /**
     * Updates statistics for all active and draining servers.
     *
     * @param clock current simulation time
     */
    public void updateStatistics(double clock) {
        activeServersStat.updateToTime(clock, activeServers.size());
        for (WebServer ws : allServers) {
            ws.updateStatistics(clock);
        }
    }

    /**
     * Updates statistics and processes jobs for all active and draining servers.
     *
     * @param elapsed time interval elapsed
     * @param nextTime simulation time at the end of the interval
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
     *
     * @param clock current simulation time
     */
    public void finalizeStatistics(double clock) {
        for (WebServer ws : allServers) {
            ws.updateStatistics(clock);
        }
    }

    /**
     * Resets statistics for all servers in the cluster and scaling counts.
     *
     * @param clock current simulation clock
     */
    public void resetStatistics(double clock) {
        for (WebServer ws : allServers) {
            ws.resetStatistics(clock);
        }
        scaleOutCount = 0;
        scaleInCount = 0;
        activeServersStat.reset();
        activeServersStat.updateToTime(clock, activeServers.size());
    }

    /**
     * Returns the total number of scale-out actions performed.
     *
     * @return the scale out count
     */
    public int getScaleOutCount() {
        return scaleOutCount;
    }

    /**
     * Returns the total number of scale-in actions performed.
     *
     * @return the scale in count
     */
    public int getScaleInCount() {
        return scaleInCount;
    }

    /**
     * Returns the average number of active servers weighted by time.
     *
     * @param currentClock current simulation clock
     * @return the average active servers
     */
    public double getAverageActiveServers(double currentClock) {
        activeServersStat.updateToTime(currentClock, activeServers.size());
        return activeServersStat.getMean();
    }
}
