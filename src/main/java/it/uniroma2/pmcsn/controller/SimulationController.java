package it.uniroma2.pmcsn.controller;

import it.uniroma2.pmcsn.lib.statistics.TimedWelford;
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
    private double totalSystemResponseTime = 0.0;

    private int totalJobsArrived = 0;
    private int totalJobsDiverted = 0;
    private int totalJobsCompleted = 0;

    // System-wide time-weighted statistics
    private final TimedWelford systemJisStat = new TimedWelford();
    private final TimedWelford systemUtilStat = new TimedWelford();

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
     * Execute the simulation based on a specific stop condition.
     *
     * @param condition Specifies when the simulation segment should terminate.
     */
    public void run(StopCondition condition) {
        scheduleInitialEvents();

        int targetJobs = Integer.MAX_VALUE;
        double targetTime = Double.MAX_VALUE;

        // Get the stop condition
        switch (condition.criteria()) {
            case JOBS_COMPLETED -> targetJobs = totalJobsCompleted + (int) condition.targetValue();
            case TIME_ELAPSED -> targetTime = clock + condition.targetValue();
            case QUEUE_EMPTY -> { /* Use default infinite value */ }
        }

        // Simulation loop
        while (totalJobsCompleted < targetJobs && clock < targetTime && processNextEvent());

        // Report finalization
        if (eventQueue.isEmpty() || clock >= maxTime) {
            webServerCluster.finalizeStatistics(clock);
            spikeServer.updateStatistics(clock);

            updateSystemStats(clock);
        }
    }

    public void scheduleInitialEvents() {
        if (clock == 0.0 && eventQueue.isEmpty()) {
            webServerCluster.updateStatistics(0.0);
            spikeServer.updateStatistics(0.0);
            updateSystemStats(0.0);
            scheduleNextArrival();
            double scaleInterval = getScaleInterval();
            if (scaleInterval > 0.0) {
                eventQueue.add(new Event(scaleInterval, EventType.SCALE_CHECK, null, null));
            }
        }
    }

    public boolean processNextEvent() {
        if (eventQueue.isEmpty()) return false;

        Event event = eventQueue.poll();
        double nextTime = event.getTime();

        if (nextTime > maxTime) {
            processActiveJobs(maxTime - clock, maxTime);
            clock = maxTime;
            return false;
        }

        processActiveJobs(nextTime - clock, nextTime);
        clock = nextTime;
        processEvent(event);
        return true;
    }

    public void resetStatistics() {
        clockSinceReset = clock;
        totalJobsArrived = totalJobsDiverted = totalJobsCompleted = 0;
        totalSystemResponseTime = 0.0;
        webServerCluster.resetStatistics(clock);
        spikeServer.resetStatistics(clock);
        
        systemJisStat.reset();
        systemUtilStat.reset();
        updateSystemStats(clock);
    }

    private void processEvent(Event event) {
        switch (event.getType()) {
            case ARRIVAL -> handleArrival(event.getJob());
            case COMPLETION -> handleCompletion(event.getJob(), event.getServer());
            case SCALE_CHECK -> handleScaleCheck();
        }
    }

    private void handleArrival(Job job) {
        totalJobsArrived++;

        Server server = loadController.routeJob(job, webServerCluster, spikeServer);
        if (server == spikeServer) totalJobsDiverted++;
        server.acceptJob(job, clock);

        rescheduleCompletions(server);
        scheduleNextArrival();

        updateSystemStats(clock);
    }

    private void handleCompletion(Job job, Server server) {
        server.completeJob(job, clock);

        totalJobsCompleted++;
        totalSystemResponseTime += job.getResponseTime();
        loadController.getHorizontalScaler().recordCompletion(clock, job.getResponseTime());

        rescheduleCompletions(server);

        updateSystemStats(clock);
    }

    private void handleScaleCheck() {

        if (loadController.evaluateScaling(clock, webServerCluster, spikeServer)) {
            rescheduleAllCompletions();
            updateSystemStats(clock);
        }

        double interval = getScaleInterval();
        if (interval > 0.0) eventQueue.add(new Event(clock + interval, EventType.SCALE_CHECK, null, null));
    }

    private void scheduleNextArrival() {
        Job nextJob = eventSource.getNextJob(lastArrivalTime);
        if (nextJob != null) {
            lastArrivalTime = nextJob.getArrivalTime();
            eventQueue.add(new Event(nextJob.getArrivalTime(), EventType.ARRIVAL, nextJob, null));
        }
    }

    private void rescheduleAllCompletions() {
        eventQueue.removeIf(e -> e.getType() == EventType.COMPLETION);
        webServerCluster.getActiveServers().forEach(this::scheduleCompletionsForServer);
        webServerCluster.getDrainingServers().forEach(this::scheduleCompletionsForServer);
        scheduleCompletionsForServer(spikeServer);
    }

    private void rescheduleCompletions(Server server) {
        eventQueue.removeIf(e -> e.getType() == EventType.COMPLETION && server.equals(e.getServer()));
        scheduleCompletionsForServer(server);
    }

    private void scheduleCompletionsForServer(Server server) {
        int n = server.getActiveJobs().size();
        if (n == 0) return;
        double speed = server.getSpeedMultiplier();
        for (Job job : server.getActiveJobs()) {
            eventQueue.add(new Event(clock + (job.getRemainingServiceDemand() * n) / speed, EventType.COMPLETION, job, server));
        }
    }

    private void processActiveJobs(double elapsed, double nextTime) {
        webServerCluster.processActiveJobs(elapsed, nextTime);
        spikeServer.updateStatistics(nextTime);
        spikeServer.processJobs(elapsed);
    }

    private void updateSystemStats(double time) {
        // JIS: sum of active jobs in ALL servers
        double currentJis = webServerCluster.getAllServers().stream().mapToDouble(s -> s.getActiveJobs().size()).sum() 
                          + spikeServer.getActiveJobs().size();
        systemJisStat.updateToTime(time, currentJis);

        // System Utilization: Average busy fraction across WEB servers only
        List<WebServer> webServers = webServerCluster.getAllServers();
        double currentUtil = webServers.isEmpty() ? 0 : 
            webServers.stream().mapToDouble(s -> s.getActiveJobs().isEmpty() ? 0.0 : 1.0).average().orElse(0);
        systemUtilStat.updateToTime(time, currentUtil);
    }

    public long getSeed() { return eventSource.getSeed(); }
    public void plantSeeds(long seed) { eventSource.plantSeeds(seed); }

    public double getClock() { return clock; }
    public double getClockSinceReset() { return clock - clockSinceReset; }
    public WebServerCluster getWebServerCluster() { return webServerCluster; }
    public SpikeServer getSpikeServer() { return spikeServer; }
    public LoadManager getLoadManager() { return loadController; }
    public int getTotalJobsArrived() { return totalJobsArrived; }
    public int getTotalJobsDiverted() { return totalJobsDiverted; }
    public int getTotalJobsCompleted() { return totalJobsCompleted; }
    public double getThroughput() {
        double d = getClockSinceReset();
        return d > 0 ? (double) totalJobsCompleted / d : 0;
    }
    public double getAverageResponseTime() {
        return totalJobsCompleted > 0 ? totalSystemResponseTime / totalJobsCompleted : 0;
    }
    public double getAverageJobsInSystem() {
        return systemJisStat.getMean();
    }
    public double getSystemUtilization() {
        return systemUtilStat.getMean();
    }
    private double getScaleInterval() {
        return loadController.getHorizontalScaler() instanceof MovingWindowHorizontalScaler m ? m.getWindowSize() : 0;
    }

    /**
     *
     *
     * @param criteria
     * @param targetValue
     */
    public record StopCondition(StopCriteria criteria, double targetValue) {

        public enum StopCriteria {
            QUEUE_EMPTY,        // Run until the queue event is empty
            JOBS_COMPLETED,     // Run until a certain number of jobs have been completed
            TIME_ELAPSED        // Run until a certain time is elapsed
        }

        public static StopCondition untilQueueEmpty() {
            return new StopCondition(StopCriteria.QUEUE_EMPTY, 0);
        }

        public static StopCondition untilJobsCompleted(int jobs) {
            return new StopCondition(StopCriteria.JOBS_COMPLETED, jobs);
        }

        public static StopCondition untilTimeElapsed(double deltaMultiplier) {
            return new StopCondition(StopCriteria.TIME_ELAPSED, deltaMultiplier);
        }
    }
}
