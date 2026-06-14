package it.uniroma2.pmcsn.controller.decorator.data;

import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.controller.decorator.SimulatorDecorator;
import it.uniroma2.pmcsn.model.server.WebServer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Decorator that collects per-server job counts to validate routing balance.
 */
public class RoutingBalanceDecorator extends SimulatorDecorator implements DataExporter {
    private final List<Map<String, Object>> snapshots = new ArrayList<>();

    public RoutingBalanceDecorator(Simulator decorated) {
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
        
        List<WebServer> servers = getWebServerCluster().getAllServers();
        for (int i = 0; i < servers.size(); i++) {
            snapshot.put("server_" + (i + 1), (double) servers.get(i).getActiveJobs().size());
        }
        
        snapshots.add(snapshot);
    }

    @Override
    public List<Map<String, Object>> getCapturedData() {
        return snapshots;
    }

    @Override
    public String[] getHeaders() {
        List<String> headers = new ArrayList<>();
        headers.add("clock");
        int maxServers = getWebServerCluster().getMaxServers();
        for (int i = 1; i <= maxServers; i++) {
            headers.add("server_" + i);
        }
        return headers.toArray(new String[0]);
    }

    @Override
    public void resetStatistics() {
        super.resetStatistics();
        snapshots.clear();
    }
}
