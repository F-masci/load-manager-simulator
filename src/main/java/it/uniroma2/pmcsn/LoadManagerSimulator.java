package it.uniroma2.pmcsn;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.CommandLineConfigParser;
import it.uniroma2.pmcsn.configs.SimulationMethod;
import it.uniroma2.pmcsn.configs.WorkloadType;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.utils.LogFactory;

/**
 * Main application entry point for the horizontal scaler simulation.
 * Delegates CLI parsing and config management, then starts the run.
 */
public class LoadManagerSimulator {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(LoadManagerSimulator.class, "SIM");

    /**
     * Main application entry point.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        new LoadManagerSimulator().start(args);
    }

    /**
     * Starts the simulation process.
     *
     * @param args Command line arguments to parse.
     */
    protected void start(String[] args) {

        // Parse configurations from command line arguments
        ApplicationConfig config = CommandLineConfigParser.parse(args);

        try {
            logger.info("==================================================================");
            logger.info("                SIMULATION CONFIGURATION OVERVIEW                ");
            logger.info("==================================================================");

            // Workload Parameters
            logger.info("[WORKLOAD]");
            logger.info("  Type:                  {}", config.load().workloadType());
            if (config.load().workloadType().isDistributionWorkload()) {
                logger.info("  Mean Interarrival:     {}", config.load().meanInterarrival());
                logger.info("  CV Interarrival:       {}", config.load().cvInterarrival());
                logger.info("  Mean Service:          {}", config.load().meanService());
                logger.info("  CV Service:            {}", config.load().cvService());
            } else if (config.load().workloadType() == WorkloadType.TRACE) {
                logger.info("  Trace Path:            {}", config.load().tracePath());
            }

            // Cluster & Routing Parameters
            logger.info("[CLUSTER & ROUTING]");
            logger.info("  Initial Web Servers:   {}", config.cluster().webServersCount());
            logger.info("  Min/Max Web Servers:   {} / {}", config.cluster().minServers(), config.cluster().maxServers());
            logger.info("  Routing Policy:        {}", config.load().routingPolicy());
            logger.info("  Spike Server Enabled:  {}", config.cluster().spikeEnabled());
            logger.info("  SI_max Threshold:      {}", config.load().siMax());
            if (config.load().siLow() != -1) {
                logger.info("  SI_low             :   {}", config.load().siLow());
            }

            // Scaling Parameters
            logger.info("[SCALING]");
            logger.info("  Horizontal Scaler:     {}", config.scaling().horizontalEnabled() ? "ENABLED" : "DISABLED");
            if (config.scaling().horizontalEnabled()) {
                logger.info("    Thresholds (Out/In): {} / {}", config.scaling().scaleOutLimit(), config.scaling().scaleInLimit());
            }
            logger.info("  Vertical Scaler:       {}", config.scaling().verticalEnabled() ? "ENABLED" : "DISABLED");
            if (config.scaling().verticalEnabled()) {
                logger.info("    Thresholds (Up/Down):{} / {}", config.scaling().spikeUpperThreshold(), config.scaling().spikeLowerThreshold());
                logger.info("    Spike CPU % (Base):  {}", config.scaling().spikeCpuPercentage() * 100);
                logger.info("    Vertical Increment:  {}", config.scaling().verticalIncrement());
            }
            logger.info("  Scaling Interval:      {}", config.scaling().scaleInterval());
            logger.info("  Cooldown Period:       {}", config.scaling().cooldown());

            // Execution Parameters
            logger.info("[EXECUTION]");
            logger.info("  Method:                {}", config.execution().method());
            logger.info("  Seed:                  {}", config.execution().seed());
            if (config.execution().method() == SimulationMethod.BATCH_MEANS) {
                logger.info("  Batches:               {}", config.execution().numBatches());
                logger.info("  Batch Size:            {}", config.execution().batchSize());
                logger.info("  Warm-up Jobs:          {}", config.execution().warmUpJobs());
            } else {
                logger.info("  Replications:          {}", config.execution().numReplications());
                if (config.execution().maxJobs() > 0) logger.info("  Max Jobs:              {}", config.execution().maxJobs());
                else logger.info("  Max Time:              {}", config.execution().maxTime());
            }
            logger.info("==================================================================\n");

            run(config);

        } catch (Exception e) {
            logger.error("Error during simulation execution: ", e);
            System.exit(1);
        }
    }

    /**
     * Executes the simulation logic based on the configuration.
     *
     * @param config The application configuration to use.
     */
    protected void run(ApplicationConfig config) {

        SimulationFacade simulator = new SimulationFacade(config);
        simulator.runSimulation();
    }
}
