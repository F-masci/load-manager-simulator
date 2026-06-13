package it.uniroma2.pmcsn;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.CommandLineConfigParser;
import it.uniroma2.pmcsn.configs.WorkloadType;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application entry point for the horizontal scaler simulation.
 * Delegates CLI parsing and config management, then starts the run.
 */
public class LoadManagerSimulator {
    private static final Logger logger = LoggerFactory.getLogger(LoadManagerSimulator.class);

    static void main(String[] args) {

        // Parse configurations from command line arguments
        ApplicationConfig config = CommandLineConfigParser.parse(args);

        try {

            switch(config.workloadType()) {
                case WorkloadType.TRACE -> logger.info("Running TRACE-DRIVEN simulation using file: {}", config.tracePath());
                case WorkloadType.HYPEREXPONENTIAL -> logger.info("Running HYPEREXPONENTIAL DISTRIBUTION-DRIVEN simulation.");
                case WorkloadType.EXPONENTIAL  -> logger.info("Running EXPONENTIAL-DRIVEN simulation.");
            }

            if (config.workloadType().isDistributionWorkload()) {
                logger.info("Parameters:");
                logger.info("  Seed: {}", config.seed());
                logger.info("  Mean Interarrival Time: {}", config.meanInterarrival());
                logger.info("  Mean Service Time: {}", config.meanService());
            }

            logger.info("  Max Time:              {}", config.maxTime());
            logger.info("  SI_max Threshold:      {}", config.siMax());
            logger.info("  Web Servers count:     {} (capacity per server={})", config.webServersCount(), config.webServerCapacity());
            logger.info("  Spike Server capacity: {}", config.spikeServerCapacity());
            logger.info("  Routing Policy:        {}", config.routingPolicy());
            logger.info("  Simulation Method:     {}", config.simulationMethod());

            SimulationFacade simulator = new SimulationFacade(config);
            simulator.runSimulation();

        } catch (Exception e) {
            logger.error("Error during simulation execution: ", e);
            System.exit(1);
        }
    }
}
