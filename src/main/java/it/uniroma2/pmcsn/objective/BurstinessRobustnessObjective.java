package it.uniroma2.pmcsn.objective;

import it.uniroma2.pmcsn.LoadManagerSimulator;
import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.*;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.utils.LogFactory;

import it.uniroma2.pmcsn.utils.objective.ObjectiveUtils;
import it.uniroma2.pmcsn.utils.chart.ObjectiveChartUtility;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Objective 4.3: Robustness to Burstiness
 * 
 * Verify if the system maintains SLA under varying levels of traffic burstiness
 * and increasing load using default full-scaling config.
 */
public class BurstinessRobustnessObjective extends LoadManagerSimulator {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(BurstinessRobustnessObjective.class, "OBJ4.3");

    public static void main(String[] args) {
        new BurstinessRobustnessObjective().start(args);
    }

    @Override
    protected void run(ApplicationConfig config) {
        logger.info("Starting Burstiness Robustness Objective (4.3)");
        
        double[] burstinessLevels = {1.0, 4.0, 8.0, 10.0, 12.0};
        double[] arrivalRates = {1.0, 2.5, 5.0, 8.0, 10.0, 12.0, 15.0};
        double slaThreshold = 5.0;

        StringBuilder report = new StringBuilder();
        StringBuilder csv = new StringBuilder("CV_Interarrival,Arrival_Rate,Average_Response_Time(R0),Status\n");
        XYSeriesCollection dataset = new XYSeriesCollection();

        report.append("\nBurstiness Robustness (Objective 4.3) Report\n");
        report.append(String.format("%-10s | %-10s | %-30s | %-10s\n", "CV", "Lambda", "R0", "Status"));
        report.append("----------------------------------------------------------------------------\n");

        for (double cv : burstinessLevels) {
            XYSeries series = new XYSeries("CV=" + cv);
            for (double lambda : arrivalRates) {
                double meanInterarrival = 1.0 / lambda;
                
                LoadConfig loadConfig = new LoadConfig(
                    meanInterarrival,
                    cv, 
                    config.load().meanService(),
                    config.load().cvService(),
                    config.load().siMax(),
                    config.load().siLow(),
                    config.load().routingPolicy(),
                    config.load().workloadType(),
                    config.load().tracePath()
                );

                // Use default full-system scaling configurations
                ApplicationConfig objectiveConfig = new ApplicationConfig(
                    loadConfig, 
                    new ApplicationConfig.ClusterConfig(), 
                    new ApplicationConfig.ScalingConfig(), 
                    config.execution(),
                    config.logging()
                );

                SimulationFacade facade = new SimulationFacade(objectiveConfig);
                SimulationFacade.AggregatedResults results = facade.runSimulation();
                
                double r0 = results.responseTime().mean();
                boolean converging = r0 < 1000.0;
                boolean feasible = r0 <= slaThreshold;
                
                double plotR0 = converging ? r0 : 50.0;
                
                report.append(String.format("%-10.1f | %-10.1f | %-30.4f | %-10s\n", cv, lambda, r0, converging ? (feasible ? "FEASIBLE" : "VIOLATED") : "NON_CONVERGING"));
                csv.append(String.format("%.1f,%.1f,%.4f,%s\n", cv, lambda, r0, converging ? (feasible ? "FEASIBLE" : "VIOLATED") : "NON_CONVERGING"));
                series.add(lambda, plotR0);
            }
            dataset.addSeries(series);
        }

        logger.info(report.toString());
        ObjectiveUtils.saveToCsv("burstiness_robustness.csv", csv.toString());
        ObjectiveChartUtility.generateBurstinessLineChart(dataset, "data/objective/burstiness_robustness.png", slaThreshold);
    }
}
