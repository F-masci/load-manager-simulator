package it.uniroma2.pmcsn.configs;

/**
 * Defines the execution method for the simulation.
 */
public enum SimulationMethod {
    /**
     * Terminating simulation with independent replications.
     */
    INDEPENDENT_REPLICATIONS,

    /**
     * Steady-state simulation using the batch means method.
     */
    BATCH_MEANS
}
