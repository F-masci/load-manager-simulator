package it.uniroma2.pmcsn.controller;

import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.event.Event;
import it.uniroma2.pmcsn.model.event.EventType;
import it.uniroma2.pmcsn.model.event.source.EventSource;
import it.uniroma2.pmcsn.model.load.LoadManager;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.MovingWindowHorizontalScaler;
import it.uniroma2.pmcsn.model.server.Server;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.PriorityQueue;

/**
 * Controller class managing the next-event driven simulation with Processor Sharing.
 * Coordinates event scheduling, routing, state updates, CPU capacity sharing, metrics reporting,
 * and system-driven horizontal/vertical scaling checks.
 */
public class SimulationController {
    private static final Logger logger = LoggerFactory.getLogger(SimulationController.class);

    private double clock = 0.0;
    private double clockSinceReset = 0.0;
    private final double maxTime;
    private final PriorityQueue<Event> eventQueue = new PriorityQueue<>();
    private final EventSource eventSource;
    private final WebServerCluster webServerCluster;
    private final SpikeServer spikeServer;
    private final LoadManager loadController;

    private double lastArrivalTime = 0.0;
    private double totalSystemResponseTime = 0.0; // tracks overall response time for completed jobs

    // Simulation metrics
    private int totalJobsArrived = 0;
    private int totalJobsDiverted = 0;
    private int totalJobsCompleted = 0;

    public SimulationController(double maxTime, EventSource eventSource,
                                WebServerCluster webServerCluster, SpikeServer spikeServer,
                                LoadManager loadController) {
        this.maxTime = maxTime;
        this.eventSource = eventSource;
        this.webServerCluster = webServerCluster;
        this.spikeServer = spikeServer;
        this.loadController = loadController;
    }

    /**
     * Starts the simulation run.
     */
    public void run() {
        scheduleInitialEvents();

        // Main next-event loop
        while (processNextEvent()) {
            // Processing...
        }

        // Finalize statistics clock alignment
        webServerCluster.finalizeStatistics(clock);
        spikeServer.updateStatistics(clock);

        // Print final simulation report
        printReport();
    }

    /**
     * Schedules the first arrival and scaling check.
     */
    public void scheduleInitialEvents() {
        if (clock == 0.0 && eventQueue.isEmpty()) {
            // Initialize statistics at t=0
            webServerCluster.updateStatistics(0.0);
            spikeServer.updateStatistics(0.0);

            // Schedule the first arrival
            scheduleNextArrival();

            // Schedule the first periodic scaling check
            double scaleInterval = getScaleInterval();
            if (scaleInterval > 0.0) {
                eventQueue.add(new Event(scaleInterval, EventType.SCALE_CHECK, null, null));
            }
        }
    }

    /**
     * Runs the simulation until a specific number of jobs are completed (warm-up).
     */
    public void runUntilWarmUp(int warmUpJobs) {
        scheduleInitialEvents();
        while (totalJobsCompleted < warmUpJobs && processNextEvent()) {
            // loop
        }
    }

    /**
     * Runs the simulation for a specific number of completed jobs (a batch).
     */
    public void runBatch(int batchSize) {
        int target = totalJobsCompleted + batchSize;
        while (totalJobsCompleted < target && processNextEvent()) {
            // loop
        }
    }

    /**
     * Processes the next event in the queue.
     * @return true if an event was processed, false if the queue is empty or maxTime reached.
     */
    public boolean processNextEvent() {
        if (eventQueue.isEmpty()) {
            return false;
        }

        Event event = eventQueue.poll();
        double nextTime = event.getTime();

        if (nextTime > maxTime) {
            // Advance clock to maxTime and process active jobs up to that point
            double elapsedToMax = maxTime - clock;
            processActiveJobs(elapsedToMax, maxTime);
            clock = maxTime;
            return false;
        }

        // Calculate elapsed time since last event
        double elapsed = nextTime - clock;
        processActiveJobs(elapsed, nextTime);
        clock = nextTime;

        processEvent(event);
        
        // Under Processor Sharing, any event changes active job counts (N),
        // requiring rescheduling of completion times for all active jobs.
        rescheduleCompletions();
        return true;
    }

    /**
     * Resets performance statistics for steady-state analysis.
     */
    public void resetStatistics() {
        clockSinceReset = clock;
        totalJobsArrived = 0;
        totalJobsDiverted = 0;
        totalJobsCompleted = 0;
        totalSystemResponseTime = 0.0;
        webServerCluster.resetStatistics(clock);
        spikeServer.resetStatistics(clock);
    }

    public double getClockSinceReset() {
        return clock - clockSinceReset;
    }

    private void processEvent(Event event) {
        switch (event.getType()) {
            case ARRIVAL:
                handleArrival(event.getJob());
                break;
            case COMPLETION:
                handleCompletion(event.getJob(), event.getServer());
                break;
            case SCALE_CHECK:
                handleScaleCheck();
                break;
        }
    }

    private void handleArrival(Job job) {
        totalJobsArrived++;
        Server actualServer = loadController.routeJob(job, webServerCluster, spikeServer);

        if (actualServer == spikeServer) {
            totalJobsDiverted++;
            logger.debug("Job #{} diverted to Spike Server at clock={}", job.getId(), clock);
        } else {
            logger.debug("Job #{} routed to Web Server #{} at clock={}", job.getId(), actualServer.getId(), clock);
        }

        // In Processor Sharing, accepted jobs enter execution immediately
        actualServer.acceptJob(job, clock);

        // Schedule the next arrival
        scheduleNextArrival();
    }

    private void handleCompletion(Job job, Server server) {
        server.completeJob(job, clock);
        totalJobsCompleted++;
        
        double respTime = job.getResponseTime();
        totalSystemResponseTime += respTime;

        // Record metrics in the horizontal scaler
        loadController.getHorizontalScaler().recordCompletion(clock, respTime);

        logger.debug("Job #{} completed on Server #{} at clock={}", job.getId(), server.getId(), clock);
    }

    private void handleScaleCheck() {
        // Trigger horizontal and vertical autoscaling checks
        boolean scaled = loadController.evaluateScaling(clock, webServerCluster, spikeServer);
        if (scaled) {
            // Under Processor Sharing, if capacity/server count changes, reschedule completions
            rescheduleCompletions();
        }

        // Schedule next check
        double scaleInterval = getScaleInterval();
        if (scaleInterval > 0.0) {
            eventQueue.add(new Event(clock + scaleInterval, EventType.SCALE_CHECK, null, null));
        }
    }

    private void scheduleNextArrival() {
        Job nextJob = eventSource.getNextJob(lastArrivalTime);
        if (nextJob != null) {
            lastArrivalTime = nextJob.getArrivalTime();
            eventQueue.add(new Event(nextJob.getArrivalTime(), EventType.ARRIVAL, nextJob, null));
        }
    }

    private void rescheduleCompletions() {
        // Remove all old completion events from the queue
        eventQueue.removeIf(e -> e.getType() == EventType.COMPLETION);

        // Schedule new completions for all active jobs in web servers
        for (WebServer ws : webServerCluster.getActiveServers()) {
            scheduleCompletionsForServer(ws);
        }
        for (WebServer ws : webServerCluster.getDrainingServers()) {
            scheduleCompletionsForServer(ws);
        }
        // Schedule completions for spike server
        scheduleCompletionsForServer(spikeServer);
    }

    private void scheduleCompletionsForServer(Server server) {
        int activeCount = server.getActiveJobs().size();
        if (activeCount == 0) {
            return;
        }
        double speed = server.getSpeedMultiplier();
        for (Job job : server.getActiveJobs()) {
            // under PS, remaining completion time = remainingDemand * N / speed
            double remainingTime = (job.getRemainingServiceDemand() * activeCount) / speed;
            double compTime = clock + remainingTime;
            eventQueue.add(new Event(compTime, EventType.COMPLETION, job, server));
        }
    }

    private void processActiveJobs(double elapsed, double nextTime) {
        webServerCluster.processActiveJobs(elapsed, nextTime);
        spikeServer.updateStatistics(nextTime);
        spikeServer.processJobs(elapsed);
    }

    /**
     * Outputs a summary of the simulation run metrics.
     */
    public void printReport() {
        logger.info("==================================================================");
        logger.info("                   SIMULATION RUN REPORT                         ");
        logger.info("==================================================================");
        logger.info(String.format("Simulation Ended At Clock: %.4f", clock));
        logger.info(String.format("Total Jobs Arrived:        %d", totalJobsArrived));
        logger.info(String.format("Total Jobs Diverted:       %d (%.2f%%)", totalJobsDiverted,
                (totalJobsArrived > 0 ? (double) totalJobsDiverted / totalJobsArrived * 100 : 0.0)));
        logger.info(String.format("Total Jobs Completed:      %d", totalJobsCompleted));
        logger.info("------------------------------------------------------------------");

        double systemTotalResponseTime = 0.0;
        int systemCompletedJobs = 0;

        logger.info("WEB SERVERS:");
        for (WebServer ws : webServerCluster.getAllServers()) {
            double util = ws.getAverageUtilization(clock);
            double avgSys = ws.getAverageSystemLength(clock);
            logger.info(String.format("  WebServer #%d (capacity=%d, speed=%.2f):", ws.getId(), ws.getCapacity(), ws.getSpeedMultiplier()));
            logger.info(String.format("    Completed Jobs:  %d", ws.getCompletedJobsCount()));
            logger.info(String.format("    Utilization:     %.4f (%.2f%%)", util, util * 100));
            logger.info(String.format("    Avg Jobs in Sys: %.4f", avgSys));
            logger.info(String.format("    Avg Resp Time:   %.4f", ws.getAverageResponseTime()));

            systemTotalResponseTime += ws.getTotalResponseTime();
            systemCompletedJobs += ws.getCompletedJobsCount();
        }

        logger.info("------------------------------------------------------------------");
        logger.info("SPIKE SERVER:");
        double spikeUtil = spikeServer.getAverageUtilization(clock);
        double spikeAvgSys = spikeServer.getAverageSystemLength(clock);
        logger.info(String.format("  SpikeServer #%d (capacity=%d, speed=%.2f):", spikeServer.getId(), spikeServer.getCapacity(), spikeServer.getSpeedMultiplier()));
        logger.info(String.format("    Completed Jobs:  %d", spikeServer.getCompletedJobsCount()));
        logger.info(String.format("    Utilization:     %.4f (%.2f%%)", spikeUtil, spikeUtil * 100));
        logger.info(String.format("    Avg Jobs in Sys: %.4f", spikeAvgSys));
        logger.info(String.format("    Avg Resp Time:   %.4f", spikeServer.getAverageResponseTime()));
        logger.info(String.format("    Avg Resources:   %.4f", spikeServer.getAverageSpeedMultiplier(clock)));

        systemTotalResponseTime += spikeServer.getTotalResponseTime();
        systemCompletedJobs += spikeServer.getCompletedJobsCount();

        logger.info("------------------------------------------------------------------");
        logger.info("AUTOSCALING STATISTICS:");
        logger.info(String.format("  Horizontal Scale Ups:   %d", webServerCluster.getScaleUpCount()));
        logger.info(String.format("  Horizontal Scale Downs: %d", webServerCluster.getScaleDownCount()));
        logger.info(String.format("  Vertical Scale Ups:     %d", loadController.getVerticalScaler().getScaleUpCount()));
        logger.info(String.format("  Vertical Scale Downs:   %d", loadController.getVerticalScaler().getScaleDownCount()));

        logger.info("------------------------------------------------------------------");
        logger.info("OVERALL SYSTEM PERFORMANCE:");
        logger.info(String.format("  Completed Jobs:      %d", systemCompletedJobs));
        logger.info(String.format("  System Avg Response: %.4f",
                (systemCompletedJobs > 0 ? systemTotalResponseTime / systemCompletedJobs : 0.0)));
        logger.info("==================================================================");
    }

    // Getters for internal states if needed by tests
    public double getClock() {
        return clock;
    }

    public List<WebServer> getWebServers() {
        return webServerCluster.getActiveServers();
    }

    public WebServerCluster getWebServerCluster() {
        return webServerCluster;
    }

    public SpikeServer getSpikeServer() {
        return spikeServer;
    }

    public LoadManager getLoadManager() {
        return loadController;
    }

    public int getTotalJobsCompleted() {
        return totalJobsCompleted;
    }

    public double getThroughput() {
        double duration = getClockSinceReset();
        return duration > 0.0 ? (double) totalJobsCompleted / duration : 0.0;
    }

    public double getAverageResponseTime() {
        return totalJobsCompleted > 0 ? totalSystemResponseTime / totalJobsCompleted : 0.0;
    }

    public double getAverageJobsInSystem() {
        double total = 0.0;
        double duration = getClockSinceReset();
        for (WebServer ws : webServerCluster.getAllServers()) {
            total += ws.getAverageSystemLength(duration);
        }
        total += spikeServer.getAverageSystemLength(duration);
        return total;
    }

    public double getSystemUtilization() {
        List<WebServer> all = webServerCluster.getAllServers();
        if (all.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        double duration = getClockSinceReset();
        for (WebServer ws : all) {
            sum += ws.getAverageUtilization(duration);
        }
        return sum / all.size();
    }

    public double getScaleInterval() {
        if (loadController.getHorizontalScaler() instanceof MovingWindowHorizontalScaler) {
            return ((MovingWindowHorizontalScaler) loadController.getHorizontalScaler()).getWindowSize();
        }
        return 0.0;
    }
}
