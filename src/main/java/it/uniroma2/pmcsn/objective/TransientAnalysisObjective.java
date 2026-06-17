package it.uniroma2.pmcsn.objective;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ClusterConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ExecutionConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.LoadConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ScalingConfig;
import it.uniroma2.pmcsn.configs.WorkloadType;
import it.uniroma2.pmcsn.controller.decorator.SimulatorDecorator;
import it.uniroma2.pmcsn.controller.decorator.data.TransientTimeSeriesCollector;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.utils.chart.ObjectiveChartUtility;
import it.uniroma2.pmcsn.utils.objective.ObjectiveUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Objective 4.2: Finite Horizon Analysis (Convergence of Replications)
 * Compares 4 architectures under 3 workload scenarios.
 * Uses standard Hyperexponential workload to observe the system starting from empty
 * and reaching a finite horizon across multiple independent replications.
 */
public class TransientAnalysisObjective extends BaseObjective {

    /**
     * Initializes the transient analysis objective.
     */
    public TransientAnalysisObjective() {
        super(TransientAnalysisObjective.class, "OBJ4.2");
    }

    /**
     * Main entry point for Objective 4.2.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        new TransientAnalysisObjective().start(args);
    }

    /**
     * Executes the transient analysis.
     *
     * @param config The application configuration.
     */
    @Override
    protected void run(ApplicationConfig config) {
        logger.info("Starting Finite Horizon Objective (4.2)...");

        try {
            Files.createDirectories(Path.of("data/objective/finite"));
        } catch (IOException e) {
            logger.error("Could not create finite objective directory");
        }

        // Workload Scenarios: (CV, Lambda)
        Map<String, double[]> scenarios = new LinkedHashMap<>();
        scenarios.put("Low_Load", new double[]{4.0, 2.5});
        scenarios.put("Medium_Load", new double[]{4.0, 5.0});
        scenarios.put("High_Load", new double[]{4.0, 8.0});

        for (String scenarioLabel : scenarios.keySet()) {
            double[] params = scenarios.get(scenarioLabel);
            double cv = params[0];
            double lambda = params[1];

            logger.info("Running Scenario: {}", scenarioLabel);

            Map<String, XYSeriesCollection> gridData = new LinkedHashMap<>();
            StringBuilder csv = new StringBuilder("Time_s,Architecture,Replication,Seed,R0_s\n");

            // Initialize datasets for the 2x2 grid
            XYSeriesCollection staticData = new XYSeriesCollection();
            XYSeriesCollection horizData = new XYSeriesCollection();
            XYSeriesCollection vertData = new XYSeriesCollection();
            XYSeriesCollection compData = new XYSeriesCollection();

            gridData.put("Static (1 WS)", staticData);
            gridData.put("Horizontal Only", horizData);
            gridData.put("Vertical Only", vertData);
            gridData.put("Complete System", compData);

            // Run Architectures
            // runArchScenario("STATIC", config, 1, 1, false, false, cv, lambda, staticData, csv);
            // runArchScenario("HORIZONTAL", config, 1, 25, true, false, cv, lambda, horizData, csv);
            // runArchScenario("VERTICAL", config, 1, 1, false, true, cv, lambda, vertData, csv);
            runArchScenario("COMPLETE", config, 1, 25, true, true, cv, lambda, compData, csv);

            ObjectiveUtils.saveToCsv("finite/transient_" + scenarioLabel.toLowerCase() + ".csv", csv.toString());
            String title = "Finite Horizon Analysis: " + scenarioLabel.replace("_", " ") + " (CV=" + cv + ", Lambda=" + lambda + ")";
            ObjectiveChartUtility.generateTransientGrid(gridData, title, "data/objective/finite/transient_" + scenarioLabel.toLowerCase() + ".png", 5.0);
        }
    }

    /**
     * Runs a specific architecture scenario for transient analysis.
     *
     * @param label           The architecture label.
     * @param baseConfig      The base application configuration.
     * @param min             Minimum number of servers.
     * @param max             Maximum number of servers.
     * @param hEnabled        Whether horizontal scaling is enabled.
     * @param vEnabled        Whether vertical scaling is enabled.
     * @param cv              Coefficient of variation for interarrival.
     * @param lambda          Arrival rate.
     * @param scenarioDataset The dataset to populate.
     * @param csv             The CSV string builder to populate.
     */
    private void runArchScenario(String label, ApplicationConfig baseConfig, 
                                 int min, int max, boolean hEnabled, boolean vEnabled,
                                 double cv, double lambda,
                                 XYSeriesCollection scenarioDataset, StringBuilder csv) {
        
        logger.info("Running Architecture: {}", label);
        
        ScalingConfig sConf = new ScalingConfig(
                ApplicationConfig.SCALE_OUT_LIMIT, ApplicationConfig.SCALE_IN_LIMIT,
                ApplicationConfig.SCALE_INTERVAL, ApplicationConfig.COOLDOWN,
                ApplicationConfig.SPIKE_UPPER_THRESHOLD, ApplicationConfig.SPIKE_LOWER_THRESHOLD,
                ApplicationConfig.SPIKE_CPU_PERCENTAGE, ApplicationConfig.VERTICAL_INCREMENT,
                hEnabled, vEnabled
        );

        ApplicationConfig fullConfig = new ApplicationConfig(
            new LoadConfig(
                    1.0 / lambda, cv,
                    ApplicationConfig.MEAN_SERVICE, ApplicationConfig.CV_SERVICE,
                    ApplicationConfig.SI_MAX, -1,
                    ApplicationConfig.ROUTING_POLICY, WorkloadType.HYPEREXPONENTIAL, null
            ),
            new ClusterConfig(min, min, max, vEnabled), 
            sConf, 
            ExecutionConfig.replications(5, 500_800.0), // 10 Replications, 50.0s each (transient)
            baseConfig.logging()
        );

        SimulationFacade facade = new SimulationFacade(fullConfig);
        facade.setCustomDecorator(TransientTimeSeriesCollector.class);
        
        facade.runSimulation();
        
        List<SimulatorDecorator> decorators = facade.getCustomDecorators();
        
        for (int i = 0; i < decorators.size(); i++) {
            TransientTimeSeriesCollector collector = (TransientTimeSeriesCollector) decorators.get(i);
            
            long runSeed = collector.getStartingSeed();
            XYSeries repSeries = new XYSeries("Rep " + (i + 1) + " (Seed: " + runSeed + ")");
            for (double[] dataPoint : collector.getTimeSeries()) {
                double t = dataPoint[0];
                double r0 = dataPoint[1];
                repSeries.add(t, r0);
                csv.append(String.format(Locale.US, "%.4f,%s,%d,%d,%.4f\n", t, label, i + 1, runSeed, r0));
            }
            scenarioDataset.addSeries(repSeries);
        }
    }
}


