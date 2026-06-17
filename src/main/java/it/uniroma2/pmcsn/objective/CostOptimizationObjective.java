package it.uniroma2.pmcsn.objective;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ClusterConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.LoadConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ScalingConfig;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.facade.SimulationFacade.AggregatedResults;
import it.uniroma2.pmcsn.utils.chart.ObjectiveChartUtility;
import it.uniroma2.pmcsn.utils.objective.ObjectiveUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Objective 3.1: Economic Analysis
 * Compares Total Cost and Response Time across different arrival rates
 * for multiple candidate horizontal scaling thresholds, across 4 different cooldown values.
 * C_tot = (N_avg * 100) + (S_avg * 100)
 */
public class CostOptimizationObjective extends BaseObjective {

    /**
     * Initializes the cost optimization objective.
     */
    public CostOptimizationObjective() {
        super(CostOptimizationObjective.class, "OBJ3.1");
    }

    /**
     * Main entry point for Objective 3.1.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        new CostOptimizationObjective().start(args);
    }

    /**
     * Executes the economic analysis.
     *
     * @param config The application configuration.
     */
    @Override
    protected void run(ApplicationConfig config) {
        logger.info("Starting Economic Analysis: Total Cost vs Load Objective (3.1)...");

        // Fixed upper threshold as per default, varying lower threshold
        double fixedUpper = ApplicationConfig.SCALE_OUT_LIMIT;
        Map<String, Double> candidates = new LinkedHashMap<>();
        candidates.put("ScaleIn 1.0", 1.0);
        candidates.put("ScaleIn 2.0", 2.0);
        candidates.put("ScaleIn 3.0", 3.0);
        candidates.put("ScaleIn 4.0", 4.0);

        double[] cooldownsMs = {5.0, 10.0, 50.0, 100.0};
        double[] lambdas = {1.0, 2.5, 5.0, 7.5, 10.0, 15.0};
        
        double costWS = 2.5;
        double costCPU = 1.0;

        StringBuilder report = new StringBuilder();
        StringBuilder csv = new StringBuilder("ScaleIn_Limit,Cooldown_S,Lambda,N_avg,S_avg,C_tot,R0\n");
        Map<Double, XYSeriesCollection> costCooldownData = new LinkedHashMap<>();
        Map<Double, XYSeriesCollection> rtCooldownData = new LinkedHashMap<>();

        report.append("\nEconomic Analysis (Objective 3.1) Report [Fixed ScaleOut = " + fixedUpper + "]\n");
        report.append(String.format("%-15s | %-5s | %-7s | %-10s | %-10s | %-10s\n", 
                "ScaleIn Limit", "CD", "Lambda", "N_avg", "C_tot", "R0"));
        report.append("--------------------------------------------------------------------------------------\n");

        for (double cooldown : cooldownsMs) {
            XYSeriesCollection currentCostDataset = new XYSeriesCollection();
            XYSeriesCollection currentRtDataset = new XYSeriesCollection();
            costCooldownData.put(cooldown, currentCostDataset);
            rtCooldownData.put(cooldown, currentRtDataset);

            for (String label : candidates.keySet()) {
                double scaleInLimit = candidates.get(label);
                XYSeries costSeries = new XYSeries(label);
                XYSeries rtSeries = new XYSeries(label);
                currentCostDataset.addSeries(costSeries);
                currentRtDataset.addSeries(rtSeries);

                for (double lambda : lambdas) {
                    ApplicationConfig objectiveConfig = new ApplicationConfig(
                            new LoadConfig(
                                    1.0 / lambda, ApplicationConfig.CV_INTERARRIVAL,
                                    ApplicationConfig.MEAN_SERVICE, ApplicationConfig.CV_SERVICE,
                                    ApplicationConfig.SI_MAX, -1,
                                    ApplicationConfig.ROUTING_POLICY, ApplicationConfig.WORKLOAD_TYPE, null
                            ),
                            new ClusterConfig(1, 1, 25, true),
                            new ScalingConfig(
                                    fixedUpper, scaleInLimit,
                                    ApplicationConfig.SCALE_INTERVAL, cooldown,
                                    ApplicationConfig.SPIKE_UPPER_THRESHOLD,
                                    ApplicationConfig.SPIKE_LOWER_THRESHOLD,
                                    ApplicationConfig.SPIKE_CPU_PERCENTAGE,
                                    ApplicationConfig.VERTICAL_INCREMENT,
                                    true, true
                            ),
                            config.execution(),
                            config.logging()
                    );

                    SimulationFacade facade = new SimulationFacade(objectiveConfig);
                    AggregatedResults results = facade.runSimulation();

                    double nAvg = results.avgServers().mean();
                    double sAvg = results.spikeAvgSpeed().mean();
                    double r0 = results.responseTime().mean();
                    double cTot = (nAvg * costWS) + (sAvg * costCPU);

                    report.append(String.format("%-15.1f | %-5.0f | %-7.1f | %-10.2f | %-10.2f | %-10.4f\n", 
                            scaleInLimit, cooldown, lambda, nAvg, cTot, r0));
                    
                    csv.append(String.format("%.1f,%.1f,%.1f,%.4f,%.4f,%.4f,%.4f\n", 
                            scaleInLimit, cooldown, lambda, nAvg, sAvg, cTot, r0));
                    
                    costSeries.add(lambda, cTot);
                    rtSeries.add(lambda, r0);
                }
            }
            logger.info("Completed analysis for Cooldown = {}s", cooldown);
        }

        logger.info(report.toString());
        ObjectiveUtils.saveToCsv("cost_analysis.csv", csv.toString());
        ObjectiveChartUtility.generateCostCooldownGrid(costCooldownData, rtCooldownData, "data/objective/cost_analysis.png", 5.0);
    }
}

