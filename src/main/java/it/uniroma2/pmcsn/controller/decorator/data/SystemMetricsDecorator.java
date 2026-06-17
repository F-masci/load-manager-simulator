package it.uniroma2.pmcsn.controller.decorator.data;

import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.controller.decorator.SimulatorDecorator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Decorator that collects basic system metrics including response time and utilization.
 */
public class SystemMetricsDecorator extends SimulatorDecorator implements DataExporter {
    private final List<Map<String, Object>> snapshots = new ArrayList<>();

    /**
     * Initializes the decorator with a simulator.
     *
     * @param decorated the simulator to decorate
     */
    public SystemMetricsDecorator(Simulator decorated) {
        super(decorated);
    }

    /**
     * Processes the next event and captures the system state.
     *
     * @return true if an event was processed, false otherwise
     */
    @Override
    public boolean processNextEvent() {
        boolean result = super.processNextEvent();
        captureState();
        return result;
    }

    /**
     * Captures a snapshot of basic system performance metrics.
     */
    private void captureState() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("clock", getClock());
        snapshot.put("event", getDecorated().getLastEventType());
        snapshot.put("rt", getAverageResponseTime());
        snapshot.put("jis", getAverageJobsInSystem());
        snapshot.put("util", getSystemUtilization());
        snapshot.put("thr", getThroughput());
        
        snapshots.add(snapshot);
    }

    /**
     * Retrieves the captured system metric snapshots.
     *
     * @return list of captured data maps
     */
    @Override
    public List<Map<String, Object>> getCapturedData() {
        return snapshots;
    }

    /**
     * Retrieves the headers for system metrics.
     *
     * @return array of header names
     */
    @Override
    public String[] getHeaders() {
        return new String[]{"clock", "event", "rt", "jis", "util", "thr"};
    }

    /**
     * Resets the collected snapshots and base statistics.
     */
    @Override
    public void resetStatistics() {
        super.resetStatistics();
        snapshots.clear();
    }
}
