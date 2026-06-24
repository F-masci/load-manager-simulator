package it.uniroma2.pmcsn.objective;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ClusterConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ScalingConfig;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.facade.SimulationFacade.AggregatedResults;
import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;
import it.uniroma2.pmcsn.utils.chart.ObjectiveChartUtility;
import it.uniroma2.pmcsn.utils.objective.ObjectiveUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Objective 1.1: Routing Policy Comparison
 * Compare routing policies on R0 and StdDev using infinite horizon.
 * Config: 4 Web Servers STATIC (min=4, max=4), Spike Server ENABLED.
 * Scaling: DISABLED.
 */
public class RoutingPolicyObjective extends BaseObjective {

    /**
     * Initializes the routing policy objective.
     */
    public RoutingPolicyObjective() {
        super(RoutingPolicyObjective.class, "OBJ1.1");
    }

    /**
     * Main entry point for Objective 2.1.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {

        final int tunedBatchSize  = 4096;
        final int tunedBatchNums  = 2048;
        final int tunedWarmupJobs = 5 * tunedBatchSize;

        ApplicationConfig config = new ApplicationConfig(
            new ApplicationConfig.LoadConfig(),
            ClusterConfig.fixedServer(4, false),
            ScalingConfig.disabled(),
            ApplicationConfig.ExecutionConfig.batchRun(tunedBatchNums, tunedBatchSize, tunedWarmupJobs)
        );

        new RoutingPolicyObjective().start(config);
    }

    /**
     * Executes the routing policy analysis using infinite horizon.
     *
     * @param config The application configuration.
     */
    @Override
    protected void run(ApplicationConfig config) {
        logger.info("Starting Routing Policy Objective...");

        StringBuilder report = new StringBuilder();
        StringBuilder csv = new StringBuilder("Policy,R0_mean,R0_hw,StdDev\n");
        Map<String, Double> r0Map = new HashMap<>();
        Map<String, Double> stdDevMap = new HashMap<>();

        report.append("\nRouting Policy Comparison Report\n");
        report.append(String.format("Batch Size: %d | Num Batches: %d | Warm-up: %d jobs\n", config.execution().batchSize(), config.execution().batchSize(), config.execution().warmUpJobs()));
        report.append(String.format("%-15s | %-10s | %-10s\n", "Policy", "R0", "StdDev"));
        report.append("-------------------------------------------\n");

        RoutingPolicy[] policies = Arrays.stream(RoutingPolicy.values())
                .filter(p -> p != RoutingPolicy.DETERMINISTIC)
                .toArray(RoutingPolicy[]::new);

        for (RoutingPolicy policy : policies) {
            ApplicationConfig.LoadConfig loadConfig = config.load();
            ApplicationConfig currentConfig = new ApplicationConfig(
                    new ApplicationConfig.LoadConfig(
                            loadConfig.workloadType(),
                            loadConfig.meanInterarrival(), loadConfig.cvInterarrival(),
                            loadConfig.meanService(), loadConfig.cvService(),
                            policy
                    ),
                    config.cluster(),
                    config.scaling(),
                    config.execution()
            );

            SimulationFacade facade = new SimulationFacade(currentConfig);
            AggregatedResults results = facade.runSimulation();

            double r0_mean = results.responseTime().mean();
            double r0_hw = results.responseTime().halfWidth();
            double stdDev = results.responseTime().stdDev();

            report.append(String.format("%-15s | %-10.4f | %-10.4f\n", policy, r0_mean, stdDev));
            csv.append(String.format(Locale.US, "%s,%.4f,%.4f,%.4f\n", policy, r0_mean, r0_hw, stdDev));

            r0Map.put(policy.name(), r0_mean);
            stdDevMap.put(policy.name(), stdDev);
        }

        logger.info(report.toString());
        ObjectiveUtils.saveToCsv("routing_policy_comparison.csv", csv.toString());
        ObjectiveChartUtility.generateRoutingStatisticalChart(r0Map, stdDevMap, "data/objective/routing_policy_comparison.png");
    }
}
