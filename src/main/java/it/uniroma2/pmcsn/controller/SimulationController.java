package it.uniroma2.pmcsn.controller;

import it.uniroma2.pmcsn.lib.statistics.TimedWelford;
import it.uniroma2.pmcsn.model.Job;
import it.uniroma2.pmcsn.model.event.Event;
import it.uniroma2.pmcsn.model.event.EventType;
import it.uniroma2.pmcsn.model.event.source.EventSource;
import it.uniroma2.pmcsn.model.load.LoadManager;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.HorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.VerticalScaler;
import it.uniroma2.pmcsn.model.server.Server;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import it.uniroma2.pmcsn.utils.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Controller class managing the next-event driven simulation with Processor Sharing.
 */
public class SimulationController implements Simulator {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(SimulationController.class, "SIM");

    private double clock = 0.0;
    private double clockSinceReset = 0.0;
    private final double maxTime;
    private final PriorityQueue<Event> eventQueue = new PriorityQueue<>();
    private final EventSource eventSource;
    private final WebServerCluster webServerCluster;
    private final SpikeServer spikeServer;
    private final boolean isSpikeEnabled;
    private final LoadManager loadController;

    private double lastArrivalTime = 0.0;
    private double totalSystemResponseTime = 0.0;

    private int totalJobsArrived = 0;
    private int totalJobsDiverted = 0;
    private int totalJobsCompleted = 0;
    private EventType lastEventType = null;

    // System-wide time-weighted statistics
    private final TimedWelford systemJisStat = new TimedWelford();
    private final TimedWelford systemUtilStat = new TimedWelford();
    private final double scalingCheckDelay;

    /**
     * Initializes the simulation controller with system components and constraints.
     *
     * @param maxTime maximum simulation time
     * @param eventSource source for job arrivals
     * @param webServerCluster cluster of web servers
     * @param spikeServer spike server instance
     * @param loadController load manager for routing and scaling
     * @param scalingCheckDelay delay between periodic scaling checks
     */
    public SimulationController(double maxTime, EventSource eventSource,
                                WebServerCluster webServerCluster, SpikeServer spikeServer, boolean isSpikeEnabled,
                                LoadManager loadController, double scalingCheckDelay) {
        this.maxTime = maxTime;
        this.eventSource = eventSource;
        this.webServerCluster = webServerCluster;
        this.spikeServer = spikeServer;
        this.isSpikeEnabled = isSpikeEnabled;
        this.loadController = loadController;
        this.scalingCheckDelay = scalingCheckDelay;
    }

    @Override
    public void scheduleInitialEvents() {
        if (clock == 0.0 && eventQueue.isEmpty()) {
            webServerCluster.updateStatistics(0.0);
            spikeServer.updateStatistics(0.0);
            updateSystemStats(0.0);
            scheduleNextArrival();
            
            if (scalingCheckDelay > 0.0) {
                eventQueue.add(new Event(scalingCheckDelay, EventType.SCALE_CHECK_HORIZONTAL, null, null));
                eventQueue.add(new Event(scalingCheckDelay, EventType.SCALE_CHECK_VERTICAL, null, null));
            }
        }
    }

    @Override
    public void finalizeSimulation() {
        // Report finalization - ALWAYS finalize to close last intervals
        webServerCluster.finalizeStatistics(clock);
        spikeServer.updateStatistics(clock);
        updateSystemStats(clock);
    }

    @Override
    public boolean processNextEvent() {
        if (eventQueue.isEmpty()) return false;

        Event event = eventQueue.poll();
        this.lastEventType = event.getType();
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

    @Override
    public void resetStatistics() {
        clockSinceReset = clock;
        totalJobsArrived = totalJobsDiverted = totalJobsCompleted = 0;
        totalSystemResponseTime = 0.0;
        webServerCluster.resetStatistics(clock);
        spikeServer.resetStatistics(clock);
        
        loadController.getHorizontalScaler().resetStatistics();
        loadController.getVerticalScaler().resetStatistics();

        systemJisStat.reset();
        systemUtilStat.reset();
        updateSystemStats(clock);
    }

    /**
     * Dispatches the event to the appropriate handler based on its type.
     *
     * @param event event to process
     */
    private void processEvent(Event event) {
        switch (event.getType()) {
            case ARRIVAL -> handleArrival(event.getJob());
            case COMPLETION -> handleCompletion(event.getJob(), event.getServer());
            case SCALE_CHECK_HORIZONTAL -> handleHorizontalScaleCheck();
            case SCALE_CHECK_VERTICAL -> handleVerticalScaleCheck();
        }
    }

    /**
     * Handles a job arrival by routing it to a server and scheduling the next completion.
     *
     * @param job arriving job
     */
    private void handleArrival(Job job) {
        totalJobsArrived++;

        Server server = loadController.routeJob(job, webServerCluster, spikeServer);
        if (server == spikeServer) totalJobsDiverted++;
        server.acceptJob(job, clock);

        rescheduleCompletions(server);
        scheduleNextArrival();

        updateSystemStats(clock);
        checkAndApplyScaling();
    }

    /**
     * Handles a job completion by releasing resources and updating statistics.
     *
     * @param job completed job
     * @param server server that processed the job
     */
    private void handleCompletion(Job job, Server server) {
        server.completeJob(job, clock);

        totalJobsCompleted++;
        totalSystemResponseTime += job.getResponseTime();
        loadController.getHorizontalScaler().recordCompletion(clock, job.getResponseTime());

        rescheduleCompletions(server);

        updateSystemStats(clock);
        checkAndApplyScaling();
    }

    /**
     * Triggers both horizontal and vertical scaling evaluation.
     */
    private void checkAndApplyScaling() {
        checkHorizontalScaling();
        checkVerticalScaling();
    }

    /**
     * Evaluates horizontal scaling and reschedules events if a change occurs.
     */
    private void checkHorizontalScaling() {
        HorizontalScaler hScaler = loadController.getHorizontalScaler();
        if (hScaler.evaluateScaling(clock, webServerCluster)) {
            rescheduleAllCompletions();
            updateSystemStats(clock);
            
            // Scaling happened, delay the periodic check by the cooldown
            double cooldown = hScaler.getCooldown();
            eventQueue.removeIf(e -> e.getType() == EventType.SCALE_CHECK_HORIZONTAL);
            eventQueue.add(new Event(clock + cooldown, EventType.SCALE_CHECK_HORIZONTAL, null, null));
        }
    }

    /**
     * Evaluates vertical scaling and reschedules events if a change occurs.
     */
    private void checkVerticalScaling() {
        VerticalScaler vScaler = loadController.getVerticalScaler();
        if (vScaler.evaluateScaling(clock, spikeServer)) {
            rescheduleAllCompletions();
            updateSystemStats(clock);
            
            // Scaling happened, delay the periodic check by the cooldown
            double cooldown = vScaler.getCooldown();
            eventQueue.removeIf(e -> e.getType() == EventType.SCALE_CHECK_VERTICAL);
            eventQueue.add(new Event(clock + cooldown, EventType.SCALE_CHECK_VERTICAL, null, null));
        }
    }

    /**
     * Handles periodic horizontal scale evaluation and reschedules the next check.
     */
    private void handleHorizontalScaleCheck() {
        checkHorizontalScaling();
        boolean hasPending = eventQueue.stream().anyMatch(e -> e.getType() == EventType.SCALE_CHECK_HORIZONTAL);
        if (!hasPending && scalingCheckDelay > 0.0) {
            eventQueue.add(new Event(clock + scalingCheckDelay, EventType.SCALE_CHECK_HORIZONTAL, null, null));
        }
    }

    /**
     * Handles periodic vertical scale evaluation and reschedules the next check.
     */
    private void handleVerticalScaleCheck() {
        checkVerticalScaling();
        boolean hasPending = eventQueue.stream().anyMatch(e -> e.getType() == EventType.SCALE_CHECK_VERTICAL);
        if (!hasPending && scalingCheckDelay > 0.0) {
            eventQueue.add(new Event(clock + scalingCheckDelay, EventType.SCALE_CHECK_VERTICAL, null, null));
        }
    }

    /**
     * Generates and schedules the next job arrival.
     */
    private void scheduleNextArrival() {
        Job nextJob = eventSource.getNextJob(lastArrivalTime);
        if (nextJob != null) {
            lastArrivalTime = nextJob.getArrivalTime();
            eventQueue.add(new Event(nextJob.getArrivalTime(), EventType.ARRIVAL, nextJob, null));
        }
    }

    /**
     * Clears and reschedules all completion events for all active servers.
     */
    private void rescheduleAllCompletions() {
        eventQueue.removeIf(e -> e.getType() == EventType.COMPLETION);
        webServerCluster.getActiveServers().forEach(this::scheduleCompletionsForServer);
        webServerCluster.getDrainingServers().forEach(this::scheduleCompletionsForServer);
        scheduleCompletionsForServer(spikeServer);
    }

    /**
     * Reschedules completion events for a specific server.
     *
     * @param server server to update
     */
    private void rescheduleCompletions(Server server) {
        eventQueue.removeIf(e -> e.getType() == EventType.COMPLETION && server.equals(e.getServer()));
        scheduleCompletionsForServer(server);
    }

    /**
     * Calculates completion times for all active jobs on a server and adds them to the queue.
     *
     * @param server server to schedule completions for
     */
    private void scheduleCompletionsForServer(Server server) {
        int n = server.getActiveJobs().size();
        if (n == 0) return;
        double speed = server.getSpeedMultiplier();
        for (Job job : server.getActiveJobs()) {
            eventQueue.add(new Event(clock + (job.getRemainingServiceDemand() * n) / speed, EventType.COMPLETION, job, server));
        }
    }

    /**
     * Advances job service progress for all active jobs based on elapsed time.
     *
     * @param elapsed time since last event
     * @param nextTime target clock time
     */
    private void processActiveJobs(double elapsed, double nextTime) {
        webServerCluster.processActiveJobs(elapsed, nextTime);
        spikeServer.updateStatistics(nextTime);
        spikeServer.processJobs(elapsed);
    }

    /**
     * Updates system-wide time-weighted statistics (JIS and Utilization).
     *
     * @param time current simulation time
     */
    private void updateSystemStats(double time) {
        // JIS: sum of active jobs in all allocated service nodes.
        // For Web Servers, this includes both active servers and draining servers.
        List<WebServer> webServers = new ArrayList<>();
        webServers.addAll(webServerCluster.getActiveServers());
        webServers.addAll(webServerCluster.getDrainingServers());

        double currentJis = webServers.stream()
                .mapToDouble(s -> s.getActiveJobs().size())
                .sum();

        if (isSpikeEnabled) {
            currentJis += spikeServer.getActiveJobs().size();
        }

        systemJisStat.updateToTime(time, currentJis);

        // Total System utilization: average busy fraction across currently allocated nodes.
        // The Spike Server is counted only when enabled in the cluster configuration.
        long busyWebServers = webServers.stream()
                .filter(s -> !s.getActiveJobs().isEmpty())
                .count();

        long busySpikeServer = isSpikeEnabled && !spikeServer.getActiveJobs().isEmpty() ? 1 : 0;

        int totalNodes = webServers.size() + (isSpikeEnabled ? 1 : 0);

        double currentUtil = totalNodes == 0
                ? 0.0
                : (double) (busyWebServers + busySpikeServer) / totalNodes;

        systemUtilStat.updateToTime(time, currentUtil);
    }

    @Override
    public long getSeed() { return eventSource.getSeed(); }

    @Override
    public EventType getLastEventType() {
        return lastEventType;
    }

    /**
     * Re-seeds the event source for independent replications.
     *
     * @param seed new random seed
     */
    public void plantSeeds(long seed) { eventSource.plantSeeds(seed); }

    @Override
    public double getClock() { return clock; }

    /**
     * Gets the simulation time elapsed since the last statistics reset.
     *
     * @return elapsed time since reset
     */
    public double getClockSinceReset() { return clock - clockSinceReset; }

    @Override
    public WebServerCluster getWebServerCluster() { return webServerCluster; }

    @Override
    public SpikeServer getSpikeServer() { return spikeServer; }

    @Override
    public LoadManager getLoadManager() { return loadController; }

    @Override
    public int getTotalJobsArrived() { return totalJobsArrived; }

    @Override
    public int getTotalJobsDiverted() { return totalJobsDiverted; }

    @Override
    public int getTotalJobsCompleted() { return totalJobsCompleted; }

    @Override
    public double getThroughput() {
        double d = getClockSinceReset();
        return d > 0 ? (double) totalJobsCompleted / d : 0;
    }

    @Override
    public double getAverageResponseTime() {
        return totalJobsCompleted > 0 ? totalSystemResponseTime / totalJobsCompleted : 0;
    }

    @Override
    public double getAverageJobsInSystem() {
        return systemJisStat.getMean();
    }

    @Override
    public double getSystemUtilization() {
        return systemUtilStat.getMean();
    }

    /**
     * Criteria and target values to determine when a simulation run should stop.
     *
     * @param criteria stopping logic
     * @param targetValue value that triggers the stop
     */
    public record StopCondition(StopCriteria criteria, double targetValue) {

        /**
         * Supported criteria for stopping a simulation run.
         */
        public enum StopCriteria {
            /** Stop when the event queue becomes empty. */
            QUEUE_EMPTY,
            /** Stop after a specific number of jobs are completed. */
            JOBS_COMPLETED,
            /** Stop after a specific amount of simulation time has passed. */
            TIME_ELAPSED
        }

        /**
         * Creates a stop condition that runs until the queue is empty.
         *
         * @return queue empty stop condition
         */
        public static StopCondition untilQueueEmpty() {
            return new StopCondition(StopCriteria.QUEUE_EMPTY, 0);
        }

        /**
         * Creates a stop condition that runs until a fixed number of jobs complete.
         *
         * @param jobs target number of jobs
         * @return jobs completed stop condition
         */
        public static StopCondition untilJobsCompleted(int jobs) {
            return new StopCondition(StopCriteria.JOBS_COMPLETED, jobs);
        }

        /**
         * Creates a stop condition that runs until a specific time limit.
         *
         * @param deltaMultiplier time duration to run
         * @return time elapsed stop condition
         */
        public static StopCondition untilTimeElapsed(double deltaMultiplier) {
            return new StopCondition(StopCriteria.TIME_ELAPSED, deltaMultiplier);
        }
    }
}
