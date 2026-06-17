package it.uniroma2.pmcsn.controller.decorator.data;

import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.controller.decorator.SimulatorDecorator;
import it.uniroma2.pmcsn.model.server.WebServer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Decorator that collects load comparison data including web server and spike server jobs.
 */
public class LoadComparisonDecorator extends SimulatorDecorator implements DataExporter {
    private final List<Map<String, Object>> snapshots = new ArrayList<>();

    /**
     * Initializes the decorator with a simulator.
     *
     * @param decorated the simulator to decorate
     */
    public LoadComparisonDecorator(Simulator decorated) {
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
     * Captures a snapshot of the current system state.
     */
    private void captureState() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        double clock = getClock();
        List<WebServer> webServers = getWebServerCluster().getAllServers();
        int webServerJobs = webServers.stream().mapToInt(s -> s.getActiveJobs().size()).sum();
        int spikeServerJobs = getSpikeServer().getActiveJobs().size();
        int totalActiveJobs = webServerJobs + spikeServerJobs;
        double avgSi = webServers.isEmpty() ? 0 : webServers.stream().mapToDouble(WebServer::getSpikeIndicator).average().orElse(0);
        int siMax = getLoadManager().getRouter().getSiMax();

        snapshot.put("clock", clock);
        snapshot.put("event", getDecorated().getLastEventType());
        snapshot.put("webServerJobs", webServerJobs);
        snapshot.put("spikeServerJobs", spikeServerJobs);
        snapshot.put("totalActiveJobs", totalActiveJobs);
        snapshot.put("avgSi", avgSi);
        snapshot.put("siMax", siMax);
        
        snapshots.add(snapshot);
    }

    /**
     * Retrieves the captured state snapshots.
     *
     * @return list of captured data maps
     */
    @Override
    public List<Map<String, Object>> getCapturedData() {
        return snapshots;
    }

    /**
     * Retrieves the headers for the captured data.
     *
     * @return array of header names
     */
    @Override
    public String[] getHeaders() {
        return new String[]{"clock", "event", "webServerJobs", "spikeServerJobs", "totalActiveJobs", "avgSi", "siMax"};
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
