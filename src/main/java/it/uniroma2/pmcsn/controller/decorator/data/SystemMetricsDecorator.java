package it.uniroma2.pmcsn.controller.decorator.data;

import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.controller.decorator.SimulatorDecorator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Decorator that collects basic system metrics (RT, JIS, Util, Thr) in memory.
 */
public class SystemMetricsDecorator extends SimulatorDecorator implements DataExporter {
    private final List<Map<String, Object>> snapshots = new ArrayList<>();

    public SystemMetricsDecorator(Simulator decorated) {
        super(decorated);
    }

    @Override
    public boolean processNextEvent() {
        boolean result = super.processNextEvent();
        captureState();
        return result;
    }

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

    @Override
    public List<Map<String, Object>> getCapturedData() {
        return snapshots;
    }

    @Override
    public String[] getHeaders() {
        return new String[]{"clock", "event", "rt", "jis", "util", "thr"};
    }

    @Override
    public void resetStatistics() {
        super.resetStatistics();
        snapshots.clear();
    }
}
