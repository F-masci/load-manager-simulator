package it.uniroma2.pmcsn.controller.decorator;

import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.model.load.LoadManager;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;

/**
 * Base abstract decorator for Simulator.
 */
public abstract class SimulatorDecorator implements Simulator {
    protected final Simulator decorated;

    protected SimulatorDecorator(Simulator decorated) {
        this.decorated = decorated;
    }

    public Simulator getDecorated() {
        return decorated;
    }

    @Override
    public boolean processNextEvent() {
        return decorated.processNextEvent();
    }

    @Override
    public void resetStatistics() {
        decorated.resetStatistics();
    }

    @Override
    public void scheduleInitialEvents() {
        decorated.scheduleInitialEvents();
    }

    @Override
    public void finalizeSimulation() {
        decorated.finalizeSimulation();
    }

    @Override public double getClock() { return decorated.getClock(); }
    @Override public int getTotalJobsArrived() { return decorated.getTotalJobsArrived(); }
    @Override public int getTotalJobsDiverted() { return decorated.getTotalJobsDiverted(); }
    @Override public int getTotalJobsCompleted() { return decorated.getTotalJobsCompleted(); }
    @Override public double getAverageResponseTime() { return decorated.getAverageResponseTime(); }
    @Override public double getAverageJobsInSystem() { return decorated.getAverageJobsInSystem(); }
    @Override public double getSystemUtilization() { return decorated.getSystemUtilization(); }
    @Override public double getThroughput() { return decorated.getThroughput(); }
    @Override public WebServerCluster getWebServerCluster() { return decorated.getWebServerCluster(); }
    @Override public SpikeServer getSpikeServer() { return decorated.getSpikeServer(); }
    @Override public LoadManager getLoadManager() { return decorated.getLoadManager(); }
    @Override public long getSeed() { return decorated.getSeed(); }
    @Override public it.uniroma2.pmcsn.model.event.EventType getLastEventType() { return decorated.getLastEventType(); }
}
