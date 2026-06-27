package it.uniroma2.pmcsn.objective;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ClusterConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.LoadConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ScalingConfig;
import it.uniroma2.pmcsn.configs.SimulationMethod;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.facade.SimulationFacade.AggregatedResults;
import it.uniroma2.pmcsn.utils.chart.ObjectiveChartUtility;
import it.uniroma2.pmcsn.utils.objective.ObjectiveUtils;
import org.jfree.data.xy.XYSeries;

import java.util.Locale;

/**
 * Objective 1.1: SI_max Estimation (What-If Analysis)
 * Vary siMax from 10 to 200 to find the maximum value where R0 <= 5.0s.
 * Config: 1 Web Server, Spike Server enabled, Scaling DISABLED.
 * Workload: Hyperexponential, lambda = 6.66 (mean = 0.15015), cv = 4.
 */
public class SiMaxEstimationObjective extends BaseObjective {

    /**
     * Initializes the SI_max estimation objective.
     */
    public SiMaxEstimationObjective() {
        super(SiMaxEstimationObjective.class, "OBJ1.2");
    }

    /**
     * Main entry point for Objective 1.1.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {

        final int tunedBatchSize  = 1_024;
        final int tunedBatchNums  = 8_192;
        final int tunedWarmupJobs = 5 * tunedBatchSize;

        LoadConfig baseLoad = new LoadConfig(
                ApplicationConfig.MEAN_INTERARRIVAL, ApplicationConfig.CV_SERVICE,
                ApplicationConfig.MEAN_SERVICE, ApplicationConfig.CV_SERVICE,
                10, ApplicationConfig.SI_LOW,
                ApplicationConfig.ROUTING_POLICY, ApplicationConfig.WORKLOAD_TYPE, null
        );

        ApplicationConfig config = new ApplicationConfig(
            baseLoad,
            ClusterConfig.fixedServer(1, true),
            ScalingConfig.disabled(),
            ApplicationConfig.ExecutionConfig.batchRun(tunedBatchNums, tunedBatchSize, tunedWarmupJobs)
        );

        new SiMaxEstimationObjective().start(config);
    }

    /**
     * Executes the SI_max estimation analysis.
     *
     * @param config The application configuration.
     */
    @Override
    protected void run(ApplicationConfig config) {
        logger.info("Starting SI_max Estimation Objective...");

        StringBuilder report = new StringBuilder();
        StringBuilder csv = new StringBuilder("siMax,R0_Mean,R0_Lower,R0_Upper,Spike_Jobs_Count,Spike_Jobs_Perc,Spike_Utilization\n");
        
        XYSeries rtMean = new XYSeries("Mean R0");
        XYSeries rtLower = new XYSeries("Lower 95% CI");
        XYSeries rtUpper = new XYSeries("Upper 95% CI");
        XYSeries spikeCount = new XYSeries("Diverted Jobs (count)");
        XYSeries spikePerc = new XYSeries("Diverted Jobs (%)");
        XYSeries spikeUtil = new XYSeries("Spike Utilization (%)");

        report.append("\nSI_max Estimation Report\n");
        report.append(String.format("%-10s | %-10s | %-10s | %-15s | %-15s | %-15s\n", 
                "siMax", "R0 Mean", "SLA Status", "Diverted Count", "Diverted %", "Spike Util"));
        report.append("--------------------------------------------------------------------------------------------------------------\n");

        for (int siMax = 10; siMax <= 150; siMax += 10) {
            ApplicationConfig.LoadConfig loadConfig = config.load();
            ApplicationConfig currentConfig = new ApplicationConfig(
                    new LoadConfig(
                            loadConfig.workloadType(),
                            loadConfig.meanInterarrival(), loadConfig.cvInterarrival(),
                            loadConfig.meanService(), loadConfig.cvService(),
                            loadConfig.routingPolicy(), siMax
                    ),
                    config.cluster(),
                    config.scaling(),
                    config.execution()
            );

            SimulationFacade facade = new SimulationFacade(currentConfig);
            AggregatedResults results = facade.runSimulation();

            double r0 = results.responseTime().mean();
            double r0L = results.responseTime().lowerBound();
            double r0U = results.responseTime().upperBound();
            
            double meanDiverted = results.divertedJobs().mean();
            double totalJobs = (currentConfig.execution().method() == SimulationMethod.BATCH_MEANS)
                    ? currentConfig.execution().batchSize()
                    : currentConfig.execution().maxJobs();
            double percDiverted = (meanDiverted / totalJobs) * 100.0;
            double meanSpikeUtil = results.spikeUtilization().mean() * 100.0;

            boolean feasible = r0 <= ApplicationConfig.SLA_THRESHOLD;

            report.append(String.format("%-10d | %-10.4f | %-10s | %-15.1f | %-15.2f%% | %-15.2f%%\n", 
                    siMax, r0, feasible ? "OK" : "VIOLATED", meanDiverted, percDiverted, meanSpikeUtil));
            
            csv.append(String.format(Locale.US, "%d,%.4f,%.4f,%.4f,%.1f,%.2f,%.2f\n", 
                    siMax, r0, r0L, r0U, meanDiverted, percDiverted, meanSpikeUtil));

            rtMean.add(siMax, r0);
            rtLower.add(siMax, r0L);
            rtUpper.add(siMax, r0U);
            spikeCount.add(siMax, meanDiverted);
            spikePerc.add(siMax, percDiverted);
            spikeUtil.add(siMax, meanSpikeUtil);
        }

        logger.info(report.toString());
        ObjectiveUtils.saveToCsv("simax_estimation.csv", csv.toString());
        ObjectiveChartUtility.generateSiMaxEstimationStackedChart(
                rtMean, rtLower, rtUpper, spikeCount, spikeUtil,
                "data/objective/simax_estimation.png", ApplicationConfig.SLA_THRESHOLD);
    }
}

