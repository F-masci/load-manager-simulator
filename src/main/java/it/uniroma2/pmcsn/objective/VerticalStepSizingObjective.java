package it.uniroma2.pmcsn.objective;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ClusterConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ScalingConfig;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.facade.SimulationFacade.AggregatedResults;
import it.uniroma2.pmcsn.utils.chart.ObjectiveChartUtility;
import it.uniroma2.pmcsn.utils.objective.ObjectiveUtils;
import org.jfree.data.xy.XYSeries;

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
        super(VerticalStepSizingObjective.class, "OBJ1.2");
    }

    /**
     * Main entry point for Objective 1.2.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        new VerticalStepSizingObjective().start(args);
    }

    /**
     * Executes the vertical step sizing analysis.
     *
     * @param config The application configuration.
     */
    @Override
    protected void run(ApplicationConfig config) {
        logger.info("Starting Vertical Step Sizing Objective (1.2)...");

        StringBuilder report = new StringBuilder();
        StringBuilder csv = new StringBuilder("Increment,R0,Spike_Avg_Speed,Spike_Utilization\n");

        XYSeries rtSeries = new XYSeries("Response Time R0");
        XYSeries speedSeries = new XYSeries("Avg Speed Multiplier");
        XYSeries utilSeries = new XYSeries("Utilization (%)");

        report.append("\nVertical Step Sizing (Objective 1.2) Report\n");
        report.append(String.format("%-10s | %-10s | %-20s | %-15s\n", "Increment", "R0", "Avg Speed", "Utilization"));
        report.append("------------------------------------------------------------------------------------\n");

        for (double inc = 0.1; inc <= 1.0; inc += 0.1) {
            ApplicationConfig currentConfig = new ApplicationConfig(
                    config.load(),
                    ClusterConfig.fixedServer(1, true),
                    ScalingConfig.onlyVertical(
                            ApplicationConfig.SPIKE_UPPER_THRESHOLD,
                            ApplicationConfig.SPIKE_LOWER_THRESHOLD,
                            ApplicationConfig.SPIKE_CPU_PERCENTAGE,
                            inc, // Vertical Increment
                            ApplicationConfig.COOLDOWN
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
            csv.append(String.format("%.2f,%.4f,%.4f,%.2f\n", inc, r0, spikeSpeed, spikeUtil));

            rtSeries.add(inc, r0);
            speedSeries.add(inc, spikeSpeed);
            utilSeries.add(inc, spikeUtil);
        }

        logger.info(report.toString());
        ObjectiveUtils.saveToCsv("vertical_step_sizing.csv", csv.toString());
        ObjectiveChartUtility.generateVerticalSizingStackedChart(
                rtSeries, speedSeries, utilSeries, 
                "data/objective/vertical_step_sizing.png", 5.0);
    }
}
