package it.uniroma2.pmcsn;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.CommandLineConfigParser;
import it.uniroma2.pmcsn.configs.WorkloadType;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.utils.LogFactory;

/**
 * Main application entry point for the horizontal scaler simulation.
 * Delegates CLI parsing and config management, then starts the run.
 */
public class LoadManagerSimulator {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(LoadManagerSimulator.class, "SIM");

    static void main(String[] args) {

        // Parse configurations from command line arguments
        ApplicationConfig config = CommandLineConfigParser.parse(args);

        try {

            switch(config.load().workloadType()) {
                case WorkloadType.TRACE -> logger.info("Running TRACE-DRIVEN simulation using file: {}", config.load().tracePath());
                case WorkloadType.HYPEREXPONENTIAL -> logger.info("Running HYPEREXPONENTIAL DISTRIBUTION-DRIVEN simulation.");
                case WorkloadType.EXPONENTIAL  -> logger.info("Running EXPONENTIAL-DRIVEN simulation.");
            }

            if (config.load().workloadType().isDistributionWorkload()) {
                logger.info("Parameters:");
                logger.info("  Seed: {}", config.execution().seed());
                logger.info("  Mean Inter Time:       {}", config.load().meanInterarrival());
                logger.info("  Mean Service Time:     {}", config.load().meanService());
            }

            logger.info("  Horizontal scaler      {}", config.scaling().horizontalEnabled());
            logger.info("  Vertical scaler        {}", config.scaling().verticalEnabled());

            logger.info("  Max Time:              {}", config.execution().maxTime());
            logger.info("  SI_max Threshold:      {}", config.load().siMax());
            logger.info("  Web Servers count:     {}", config.cluster().webServersCount());
            logger.info("  Routing Policy:        {}", config.load().routingPolicy());
            logger.info("  Simulation Method:     {}", config.execution().method());

            run(config);

        } catch (Exception e) {
            logger.error("Error during simulation execution: ", e);
            System.exit(1);
        }
    }

    protected static void run(ApplicationConfig config) {
        SimulationFacade simulator = new SimulationFacade(config);
        simulator.runSimulation();
    }
}
