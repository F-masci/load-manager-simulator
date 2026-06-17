package it.uniroma2.pmcsn.controller.decorator.data;

import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.controller.decorator.SimulatorDecorator;
import it.uniroma2.pmcsn.model.event.EventType;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.HorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.VerticalScaler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Decorator that collects horizontal and vertical scaling metrics in memory.
 */
public class ScalingMetricsDecorator extends SimulatorDecorator implements DataExporter {
    private final List<Map<String, Object>> snapshots = new ArrayList<>();

    /**
     * Initializes the scaling metrics decorator with a simulator.
     *
     * @param decorated the simulator to decorate
     */
    public ScalingMetricsDecorator(Simulator decorated) {
        super(decorated);
    }

    /**
     * Processes the next event and captures scaling states if changes occur.
     *
     * @return true if an event was processed, false otherwise
     */
    @Override
    public boolean processNextEvent() {
        HorizontalScaler hScaler = getLoadManager().getHorizontalScaler();
        VerticalScaler vScaler = getLoadManager().getVerticalScaler();

        double lastHScaling = hScaler.getLastScalingTime();
        double lastVScaling = vScaler.getLastScalingTime();

        boolean result = super.processNextEvent();
        
        double currentHScaling = hScaler.getLastScalingTime();
        double currentVScaling = vScaler.getLastScalingTime();
        
        boolean hScaled = currentHScaling != lastHScaling;
        boolean vScaled = currentVScaling != lastVScaling;

        // Determine which scaler was checked based on event type
        EventType lastEvent = getDecorated().getLastEventType();
        String checkType = "NONE";
        if (lastEvent != null) {
            switch (lastEvent) {
                case ARRIVAL, COMPLETION -> checkType = "BOTH";
                case SCALE_CHECK_HORIZONTAL -> checkType = "HORIZONTAL";
                case SCALE_CHECK_VERTICAL -> checkType = "VERTICAL";
            }
        }

        // If scaling happened, insert a record with cooldown forced to zero 
        if (hScaled || vScaled) {
            captureState(hScaled, vScaled, checkType);
        }

        // Standard capture
        captureState(false, false, checkType);
        
        return result;
    }

    /**
     * Captures the current state of scalers and system metrics.
     *
     * @param forceHZero true to force horizontal cooldown to zero
     * @param forceVZero true to force vertical cooldown to zero
     * @param checkType the type of scaling check performed
     */
    private void captureState(boolean forceHZero, boolean forceVZero, String checkType) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        double clock = getClock();
        
        HorizontalScaler hScaler = getLoadManager().getHorizontalScaler();
        VerticalScaler vScaler = getLoadManager().getVerticalScaler();

        snapshot.put("clock", clock);
        snapshot.put("event", getDecorated().getLastEventType());
        snapshot.put("scalingCheck", checkType);
        
        // Horizontal Scaling Metrics
        snapshot.put("activeWebServers", getWebServerCluster().getActiveServers().size());
        snapshot.put("hMetric", hScaler.getCurrentMetric(clock));
        snapshot.put("hScaleOutLimit", hScaler.getScaleOutThreshold());
        snapshot.put("hScaleInLimit", hScaler.getScaleInThreshold());
        snapshot.put("hCooldown", forceHZero ? 0.0 : hScaler.getRemainingCooldown(clock));

        // Vertical Scaling Metrics
        snapshot.put("spikeSpeed", getSpikeServer().getSpeedMultiplier());
        snapshot.put("vMetric", vScaler.getCurrentMetric(clock, getSpikeServer()));
        snapshot.put("vUpperLimit", vScaler.getUpperThreshold());
        snapshot.put("vLowerLimit", vScaler.getLowerThreshold());
        snapshot.put("vCooldown", forceVZero ? 0.0 : vScaler.getRemainingCooldown(clock));

        snapshots.add(snapshot);
    }

    /**
     * Retrieves the captured scaling snapshots.
     *
     * @return list of state snapshots
     */
    @Override
    public List<Map<String, Object>> getCapturedData() {
        return snapshots;
    }

    /**
     * Retrieves the headers for scaling metrics.
     *
     * @return array of header strings
     */
    @Override
    public String[] getHeaders() {
        return new String[]{
            "clock", "event", "scalingCheck",
            "activeWebServers", "hMetric", "hScaleOutLimit", "hScaleInLimit", "hCooldown",
            "spikeSpeed", "vMetric", "vUpperLimit", "vLowerLimit", "vCooldown"
        };
    }

    /**
     * Resets the captured scaling data and base statistics.
     */
    @Override
    public void resetStatistics() {
        super.resetStatistics();
        snapshots.clear();
    }
}

