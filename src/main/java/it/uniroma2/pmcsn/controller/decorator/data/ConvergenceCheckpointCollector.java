package it.uniroma2.pmcsn.controller.decorator.data;

import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.controller.decorator.SimulatorDecorator;

import java.util.Arrays;

/**
 * Decorator that samples cumulative simulation metrics at predefined completed-job checkpoints.
 * It is configured before a convergence campaign and then instantiated once per replication
 * by {@code SimulationFacade}.
 */
public class ConvergenceCheckpointCollector extends SimulatorDecorator {
    private static int[] configuredCheckpoints = new int[0];

    private final int[] checkpoints;
    private final double[] responseTimes;
    private final double[] throughputs;
    private final double[] clocks;
    private int nextCheckpointIndex = 0;

    /**
     * Configures the checkpoint grid used by subsequently created collectors.
     *
     * @param checkpoints completed-job checkpoints
     */
    public static void configure(int[] checkpoints) {
        configuredCheckpoints = Arrays.copyOf(checkpoints, checkpoints.length);
    }

    /**
     * Initializes the collector.
     *
     * @param decorated simulator to decorate
     */
    public ConvergenceCheckpointCollector(Simulator decorated) {
        super(decorated);
        this.checkpoints = Arrays.copyOf(configuredCheckpoints, configuredCheckpoints.length);
        this.responseTimes = new double[checkpoints.length];
        this.throughputs = new double[checkpoints.length];
        this.clocks = new double[checkpoints.length];
    }

    @Override
    public boolean processNextEvent() {
        boolean hasNext = super.processNextEvent();
        collectReachedCheckpoints();
        return hasNext;
    }

    @Override
    public void finalizeSimulation() {
        collectReachedCheckpoints();
        super.finalizeSimulation();
    }

    public double[] getResponseTimes() {
        return Arrays.copyOf(responseTimes, responseTimes.length);
    }

    public double[] getThroughputs() {
        return Arrays.copyOf(throughputs, throughputs.length);
    }

    public double[] getClocks() {
        return Arrays.copyOf(clocks, clocks.length);
    }

    private void collectReachedCheckpoints() {
        while (nextCheckpointIndex < checkpoints.length
                && getTotalJobsCompleted() >= checkpoints[nextCheckpointIndex]) {
            responseTimes[nextCheckpointIndex] = getAverageResponseTime();
            throughputs[nextCheckpointIndex] = getThroughput();
            clocks[nextCheckpointIndex] = getClock();
            nextCheckpointIndex++;
        }
    }
}
