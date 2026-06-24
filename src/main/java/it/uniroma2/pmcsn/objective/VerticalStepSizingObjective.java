package it.uniroma2.pmcsn.objective;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ClusterConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ScalingConfig;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.facade.SimulationFacade.AggregatedResults;
import it.uniroma2.pmcsn.utils.chart.ObjectiveChartUtility;
import it.uniroma2.pmcsn.utils.objective.ObjectiveUtils;
import org.jfree.data.xy.XYSeries;

import java.util.Locale;

/**
 * Objective 1.2: Vertical Step Sizing
 * Test different step increments to compare reactivity vs waste.
 * Config: 1 Web Server, Spike Server enabled.
 * Scaling: Horizontal DISABLED, Vertical ENABLED.
 */
public class VerticalStepSizingObjective extends BaseObjective {

    /**
     * Initializes the vertical step sizing objective.
     */
    public VerticalStepSizingObjective() {
        super(VerticalStepSizingObjective.class, "OBJ1.3");
    }

    /**
     * Main entry point for Objective 1.2.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {

        final int tunedBatchSize  = 8_192;
        final int tunedBatchNums  = 2_048;
        final int tunedWarmupJobs = 5 * tunedBatchSize;

        ApplicationConfig config = new ApplicationConfig(
                new ApplicationConfig.LoadConfig(),
                ClusterConfig.fixedServer(1, true),
                ScalingConfig.onlyVertical(
                        ApplicationConfig.SPIKE_UPPER_THRESHOLD,
                        ApplicationConfig.SPIKE_LOWER_THRESHOLD,
                        ApplicationConfig.SPIKE_CPU_PERCENTAGE,
                        0.0, // Dummy
                        ApplicationConfig.COOLDOWN
                ),
                ApplicationConfig.ExecutionConfig.batchRun(tunedBatchNums, tunedBatchSize, tunedWarmupJobs)
        );

        new VerticalStepSizingObjective().start(config);
    }

    /**
     * Executes the vertical step sizing analysis.
     *
     * @param config The application configuration.
     */
    @Override
    protected void run(ApplicationConfig config) {
        logger.info("Starting Vertical Step Sizing Objective...");

        StringBuilder report = new StringBuilder();
        StringBuilder csv = new StringBuilder("Increment,R0_mean,R0_hw,Spike_Avg_Speed_mean,Spike_Avg_Speed_hw,Spike_Utilization_mean,Spike_Utilization_hw\n");

        XYSeries rtSeries = new XYSeries("Response Time R0");
        XYSeries speedSeries = new XYSeries("Avg Speed Multiplier");
        XYSeries utilSeries = new XYSeries("Utilization (%)");

        report.append("\nVertical Step Sizing Report\n");
        report.append(String.format("%-10s | %-10s | %-20s | %-15s\n", "Increment", "R0", "Avg Speed", "Utilization"));
        report.append("------------------------------------------------------------------------------------\n");

        for (double inc = 0.5; inc <= 5.0; inc += 0.5) {
            ApplicationConfig currentConfig = new ApplicationConfig(
                    config.load(),
                    config.cluster(),
                    ScalingConfig.onlyVertical(
                            ApplicationConfig.SPIKE_UPPER_THRESHOLD,
                            ApplicationConfig.SPIKE_LOWER_THRESHOLD,
                            ApplicationConfig.SPIKE_CPU_PERCENTAGE,
                            inc, // Vertical Increment
                           0.0 // No Cooldown
                    ),
                    config.execution(),
                    config.logging()
            );

            SimulationFacade facade = new SimulationFacade(currentConfig);
            AggregatedResults results = facade.runSimulation();

            double r0 = results.responseTime().mean();
            double spikeSpeed = results.spikeAvgSpeed().mean();
            double spikeUtil = results.spikeUtilization().mean() * 100.0;

            report.append(String.format("%-10.2f | %-10.4f | %-20.4f | %-15.2f%%\n", inc, r0, spikeSpeed, spikeUtil));
            csv.append(String.format(Locale.US, "%.2f,%.4f,%.4f,%.4f,%.4f,%.2f,%.2f\n", inc,
                    r0, results.responseTime().halfWidth(),
                    spikeSpeed, results.spikeAvgSpeed().halfWidth(),
                    spikeUtil, results.spikeUtilization().halfWidth() * 100.0
            ));

            rtSeries.add(inc, r0);
            speedSeries.add(inc, spikeSpeed);
            utilSeries.add(inc, spikeUtil);
        }

        logger.info(report.toString());
        ObjectiveUtils.saveToCsv("vertical_step_sizing.csv", csv.toString());
        ObjectiveChartUtility.generateVerticalSizingStackedChart(
                rtSeries, speedSeries, utilSeries, 
                "data/objective/vertical_step_sizing.png", ApplicationConfig.SLA_THRESHOLD);
    }
}
