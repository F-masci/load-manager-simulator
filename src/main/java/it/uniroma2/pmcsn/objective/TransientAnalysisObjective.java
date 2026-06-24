package it.uniroma2.pmcsn.objective;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ClusterConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ExecutionConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.LoadConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ScalingConfig;
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
import java.util.List;
import java.util.Locale;

/**
 * Objectiv3.2: Finite Horizon Analysis (Convergence of Replications)
 * Studies the complete architecture under multiple workload scenarios.
 * Uses standard Hyperexponential workload to observe the system starting from empty
 * and reaching a finite horizon across multiple independent replications.
 */
public class TransientAnalysisObjective extends BaseObjective {

    private record TransientScenario(String label, double cv, double lambda) {
        String fileSuffix() {
            return label.toLowerCase(Locale.ROOT);
        }

        String chartTitle() {
            return String.format(Locale.US,
                    "Finite Horizon Analysis: %s (CV=%.1f, Lambda=%.1f)",
                    label.replace("_", " "), cv, lambda);
        }
    }

    /**
     * Initializes the transient_h.txt analysis objective.
     */
    public TransientAnalysisObjective() {
        super(TransientAnalysisObjective.class, "OBJ3.2");
    }

    /**
     * Main entry point for Objective 3.2.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        new TransientAnalysisObjective().start(new ApplicationConfig());
    }

    /**
     * Executes the transient_h.txt analysis.
     *
     * @param config The application configuration.
     */
    @Override
    protected void run(ApplicationConfig config) {
        logger.info("Starting Finite Horizon Objective...");

        try {
            Files.createDirectories(Path.of("data/objective/finite"));
        } catch (IOException e) {
            logger.error("Could not create finite objective directory");
        }

        List<TransientScenario> scenarios = List.of(
                new TransientScenario("Low_Load_CV2", 2.0, 2.5),
                new TransientScenario("Low_Load_CV4", 4.0, 2.5),
                new TransientScenario("Medium_Load_CV4", 4.0, 5.0),
                new TransientScenario("Medium_Load_CV6", 6.0, 5.0)
                // new TransientScenario("High_Load_CV4", 4.0, 8.0),
                // new TransientScenario("High_Load_CV6", 6.0, 8.0)
        );

        for (TransientScenario scenario : scenarios) {
            logger.info("Running Scenario: {}", scenario.label());

            StringBuilder csv = new StringBuilder("Time_s,Architecture,Replication,Seed,R0_s\n");

            XYSeriesCollection completeData = new XYSeriesCollection();

            runCompleteScenario(config, scenario.cv(), scenario.lambda(), completeData, csv);

            ObjectiveUtils.saveToCsv("finite/transient_" + scenario.fileSuffix() + ".csv", csv.toString());
            ObjectiveChartUtility.generateTransientChart(
                    completeData,
                    scenario.chartTitle(),
                    "data/objective/finite/transient_" + scenario.fileSuffix() + ".png",
                    ApplicationConfig.SLA_THRESHOLD
            );
        }
    }

    /**
     * Runs the complete architecture for transient_h.txt analysis.
     *
     * @param baseConfig      The base application configuration.
     * @param cv              Coefficient of variation for interarrival.
     * @param lambda          Arrival rate.
     * @param scenarioDataset The dataset to populate.
     * @param csv             The CSV string builder to populate.
     */
    private void runCompleteScenario(ApplicationConfig baseConfig, double cv, double lambda,
                                     XYSeriesCollection scenarioDataset, StringBuilder csv) {
        
        logger.info("Running Complete System: lambda={}, cv={}", lambda, cv);

        LoadConfig loadConfig = baseConfig.load();
        ApplicationConfig fullConfig = new ApplicationConfig(
            new LoadConfig(
                    1.0 / lambda, cv,
                    loadConfig.meanService(), loadConfig.cvService(),
                    loadConfig.siMax(), loadConfig.siLow(),
                    loadConfig.routingPolicy(), loadConfig.workloadType(), loadConfig.tracePath()
            ),
            new ClusterConfig(),
            new ScalingConfig(),
            ExecutionConfig.replications(10, 1_008_000.0),
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
                csv.append(String.format(Locale.US, "%.4f,%s,%d,%d,%.4f\n", t, "COMPLETE", i + 1, runSeed, r0));
            }
            scenarioDataset.addSeries(repSeries);
        }
    }
}


