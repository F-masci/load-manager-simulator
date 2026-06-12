package it.uniroma2.pmcsn;

import it.uniroma2.pmcsn.builder.SimulationBuilder;
import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.CommandLineConfigParser;
import it.uniroma2.pmcsn.controller.SimulationController;
import it.uniroma2.pmcsn.configs.WorkloadType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application entry point for the horizontal scaler simulation.
 * Delegates CLI parsing and config management, then starts the run.
 */
public class HorizontalScalerSimulator {
    private static final Logger logger = LoggerFactory.getLogger(HorizontalScalerSimulator.class);

    public static void main(String[] args) {
        // Parse configurations from command line arguments
        ApplicationConfig config = CommandLineConfigParser.parse(args);

        SimulationBuilder builder = new SimulationBuilder();

        try {
            builder.config(config);

            if (config.workloadType() == WorkloadType.TRACE) {
                logger.info("Running TRACE-DRIVEN simulation using file: {}", config.tracePath());
            } else if (config.workloadType() == WorkloadType.HYPEREXPONENTIAL) {
                logger.info("Running HYPEREXPONENTIAL DISTRIBUTION-DRIVEN simulation.");
            } else {
                logger.info("Running EXPONENTIAL DISTRIBUTION-DRIVEN simulation.");
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

            SimulationController controller = builder.build();
            controller.run();

        } catch (Exception e) {
            logger.error("Error during simulation execution: ", e);
            System.exit(1);
        }
    }
}
