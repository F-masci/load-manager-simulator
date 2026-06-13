package it.uniroma2.pmcsn.controller;

import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;
import it.uniroma2.pmcsn.model.load.LoadManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to print detailed reports of individual simulation runs.
 */
public class SimulationReporter {
    private static final Logger logger = LoggerFactory.getLogger(SimulationReporter.class);

    public static void printReport(SimulationController controller) {
        double clock = controller.getClock();
        WebServerCluster webServerCluster = controller.getWebServerCluster();
        SpikeServer spikeServer = controller.getSpikeServer();
        LoadManager loadController = controller.getLoadManager();

        logger.info("==================================================================");
        logger.info("                   SIMULATION RUN REPORT                         ");
        logger.info("==================================================================");
        logger.info(String.format("Simulation Ended At Clock: %.4f", clock));
        logger.info(String.format("Total Jobs Arrived:        %d", controller.getTotalJobsArrived()));
        logger.info(String.format("Total Jobs Diverted:       %d (%.2f%%)", controller.getTotalJobsDiverted(),
                (controller.getTotalJobsArrived() > 0 ? (double) controller.getTotalJobsDiverted() / controller.getTotalJobsArrived() * 100 : 0.0)));
        logger.info(String.format("Total Jobs Completed:      %d", controller.getTotalJobsCompleted()));
        logger.info("------------------------------------------------------------------");

        logger.info("WEB SERVERS:");
        for (WebServer ws : webServerCluster.getAllServers()) {
            double util = ws.getAverageUtilization(clock);
            double avgSys = ws.getAverageSystemLength(clock);
            logger.info(String.format("  WebServer #%d (speed=%.2f):", ws.getId(), ws.getSpeedMultiplier()));
            logger.info(String.format("    Completed Jobs:  %d", ws.getCompletedJobsCount()));
            logger.info(String.format("    Avg Active Jobs: %.4f", util));
            logger.info(String.format("    Avg Jobs in Sys: %.4f", avgSys));
            logger.info(String.format("    Avg Resp Time:   %.4f", ws.getAverageResponseTime()));
        }

        logger.info("------------------------------------------------------------------");
        logger.info("SPIKE SERVER:");
        double spikeUtil = spikeServer.getAverageUtilization(clock);
        double spikeAvgSys = spikeServer.getAverageSystemLength(clock);
        logger.info(String.format("  SpikeServer #%d (speed=%.2f):", spikeServer.getId(), spikeServer.getSpeedMultiplier()));
        logger.info(String.format("    Completed Jobs:  %d", spikeServer.getCompletedJobsCount()));
        logger.info(String.format("    Avg Active Jobs: %.4f", spikeUtil));
        logger.info(String.format("    Avg Jobs in Sys: %.4f", spikeAvgSys));
        logger.info(String.format("    Avg Resp Time:   %.4f", spikeServer.getAverageResponseTime()));
        logger.info(String.format("    Avg Resources:   %.4f", spikeServer.getAverageSpeedMultiplier(clock)));

        logger.info("------------------------------------------------------------------");
        logger.info("AUTOSCALING STATISTICS:");
        logger.info(String.format("  Horizontal Scale Ups:   %d", webServerCluster.getScaleUpCount()));
        logger.info(String.format("  Horizontal Scale Downs: %d", webServerCluster.getScaleDownCount()));
        logger.info(String.format("  Vertical Scale Ups:     %d", loadController.getVerticalScaler().getScaleUpCount()));
        logger.info(String.format("  Vertical Scale Downs:   %d", loadController.getVerticalScaler().getScaleDownCount()));

        logger.info("------------------------------------------------------------------");
        logger.info("OVERALL SYSTEM PERFORMANCE:");
        logger.info(String.format("  Completed Jobs:      %d", controller.getTotalJobsCompleted()));
        logger.info(String.format("  System Avg Response: %.4f", controller.getAverageResponseTime()));
        logger.info("==================================================================");
    }
}
