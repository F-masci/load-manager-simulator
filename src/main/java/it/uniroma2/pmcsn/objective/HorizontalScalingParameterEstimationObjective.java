package it.uniroma2.pmcsn.objective;

import it.uniroma2.pmcsn.LoadManagerSimulator;
import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;
import it.uniroma2.pmcsn.utils.LogFactory;
import it.uniroma2.pmcsn.utils.objective.ObjectiveUtils;
import it.uniroma2.pmcsn.utils.chart.ObjectiveChartUtility;
import org.jfree.data.xy.XYSeries;

import java.util.*;

/**
 * Objective 1.3: Horizontal Scaler Parameter Estimation
 * Estimate optimal thresholds for horizontal scaling by analyzing fixed configurations.
 * Compares 1, 2, 3, 4, 5 Web Servers across different arrival rates.
 * Scalers are DISABLED to identify physical limits.
 */
public class HorizontalScalingParameterEstimationObjective extends LoadManagerSimulator {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(HorizontalScalingParameterEstimationObjective.class, "OBJ1.3");

    public static void main(String[] args) {
        new HorizontalScalingParameterEstimationObjective().start(args);
    }

    @Override
    protected void run(ApplicationConfig config) {
        logger.info("Starting Horizontal Scaling Parameter Estimation Objective (1.3)...");

        double arrivalCv = ApplicationConfig.CV_INTERARRIVAL;
        double[] lambdas = {1.0, 2.5, 5.0, 10.0, 20.0};
        int[] serverCounts = {1, 2, 3, 4, 5};

        StringBuilder report = new StringBuilder();
        StringBuilder csv = new StringBuilder("Arrival_CV,Lambda,Num_Servers,R0_Mean,Status\n");
        Map<Double, Map<Integer, XYSeries>> cvResults = new LinkedHashMap<>();

        report.append("\nHorizontal Scaling Parameter Estimation (Objective 1.3) Report\n");
        report.append("------------------------------------------------------------------------------------\n");

        Map<Integer, XYSeries> wsSeriesMap = new HashMap<>();
        cvResults.put(arrivalCv, wsSeriesMap);

        for (int n : serverCounts) {
            XYSeries series = new XYSeries(n + " WS");
            wsSeriesMap.put(n, series);

            for (double lambda : lambdas) {
                ApplicationConfig currentConfig = new ApplicationConfig(
                        new ApplicationConfig.LoadConfig(
                                1.0 / lambda, arrivalCv,
                                ApplicationConfig.MEAN_SERVICE, ApplicationConfig.CV_SERVICE,
                                ApplicationConfig.SI_MAX, -1,
                                RoutingPolicy.ROUND_ROBIN, ApplicationConfig.WORKLOAD_TYPE, null
                        ),
                        ApplicationConfig.ClusterConfig.fixedServer(n, true),
                        ApplicationConfig.ScalingConfig.disabled(),
                        config.execution(),
                        config.logging()
                );

                SimulationFacade facade = new SimulationFacade(currentConfig);
                SimulationFacade.AggregatedResults results = facade.runSimulation();

                double r0 = results.responseTime().mean();
                // Simple check for non-convergence
                boolean converging = r0 < 1000.0;
                
                // If it doesn't converge, we cap it for the chart to indicate 'out of scale'
                double plotR0 = converging ? r0 : 50.0;
                series.add(lambda, plotR0);
                
                csv.append(String.format("%.1f,%.2f,%d,%.4f,%s\n", 
                        arrivalCv, lambda, n, r0, converging ? "CONVERGED" : "NON_CONVERGING"));
            }
        }
        logger.info("Completed analysis for Arrival CV = {}", arrivalCv);

        logger.info(report.toString());
        ObjectiveUtils.saveToCsv("horizontal_parameter_estimation.csv", csv.toString());
        ObjectiveChartUtility.generateHorizontalParameterEstimationChart(
                cvResults, "data/objective/horizontal_parameter_estimation.png", 5.0);
    }
}
