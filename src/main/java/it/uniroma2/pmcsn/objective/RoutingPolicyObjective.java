package it.uniroma2.pmcsn.objective;

import it.uniroma2.pmcsn.builder.SimulationBuilder;
import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ClusterConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ExecutionConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.LoadConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.LoggingConfig;
import it.uniroma2.pmcsn.configs.ApplicationConfig.ScalingConfig;
import it.uniroma2.pmcsn.configs.LoggingDataType;
import it.uniroma2.pmcsn.controller.SimulationController.StopCondition;
import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.controller.decorator.SimulatorDecorator;
import it.uniroma2.pmcsn.controller.decorator.TimeSerieCollector;
import it.uniroma2.pmcsn.lib.statistics.AutoCorrelation;
import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;
import it.uniroma2.pmcsn.utils.chart.ObjectiveChartUtility;
import it.uniroma2.pmcsn.utils.objective.ObjectiveUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Objective 2.1: Routing Policy Comparison
 * Compare outing policy on R0, StdDev, and 99th percentile.
 * Config: 3 Web Servers STATIC (min=3, max=3), Spike Server ENABLED.
 * Scaling: DISABLED.
 */
public class RoutingPolicyObjective extends BaseObjective {

    /**
     * Initializes the routing policy objective.
     */
    public RoutingPolicyObjective() {
        super(RoutingPolicyObjective.class, "OBJ2.1");
    }

    /**
     * Main entry point for Objective 2.1.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        new RoutingPolicyObjective().start(args);
    }

    /**
     * Executes the routing policy analysis.
     *
     * @param config The application configuration.
     */
    @Override
    protected void run(ApplicationConfig config) {
        logger.info("Starting Routing Policy Objective (2.1)...");

        StringBuilder report = new StringBuilder();
        StringBuilder csv = new StringBuilder("Policy,R0,StdDev,P99\n");
        Map<String, Double> r0Map = new HashMap<>();
        Map<String, Double> stdDevMap = new HashMap<>();
        Map<String, Double> p99Map = new HashMap<>();

        report.append("\nRouting Policy Comparison (Objective 2.1) Report\n");
        report.append(String.format("%-15s | %-10s | %-10s | %-10s\n", "Policy", "R0", "StdDev", "P99"));
        report.append("-------------------------------------------------------------\n");

        RoutingPolicy[] policies = Arrays.stream(RoutingPolicy.values())
                .filter(p -> p != RoutingPolicy.DETERMINISTIC)
                .toArray(RoutingPolicy[]::new);

        for (RoutingPolicy policy : policies) {
            ApplicationConfig currentConfig = new ApplicationConfig(
                    new LoadConfig(
                            config.load().workloadType(),
                            config.load().meanInterarrival(),
                            config.load().meanService(),
                            policy,
                            config.load().siMax()
                    ),
                    ClusterConfig.fixedServer(4, true),
                    ScalingConfig.disabled(),
                    ExecutionConfig.singleRun(1_000_000),
                    new LoggingConfig(true, config.logging().format(), LoggingDataType.TIME_SERIE, config.logging().outputPath())
            );

            SimulationBuilder builder = new SimulationBuilder().config(currentConfig);
            Simulator controller = builder.build();

            TimeSerieCollector collector = findCollector(controller);
            if (collector == null) {
                logger.error("TimeSerieCollector not found in controller!");
                continue;
            }

            controller.run(StopCondition.untilJobsCompleted(currentConfig.execution().maxJobs()));

            List<Double> series = collector.getSeries();
            double r0 = controller.getAverageResponseTime();
            double stdDev = AutoCorrelation.calculateStdDev(series, r0);
            double p99 = AutoCorrelation.calculatePercentile(series, 99);

            report.append(String.format("%-15s | %-10.4f | %-10.4f | %-10.4f\n", policy, r0, stdDev, p99));
            csv.append(String.format("%s,%.4f,%.4f,%.4f\n", policy, r0, stdDev, p99));

            r0Map.put(policy.name(), r0);
            stdDevMap.put(policy.name(), stdDev);
            p99Map.put(policy.name(), p99);
        }

        logger.info(report.toString());
        ObjectiveUtils.saveToCsv("routing_policy_comparison.csv", csv.toString());
        ObjectiveChartUtility.generateRoutingStatisticalChart(r0Map, stdDevMap, p99Map, "data/objective/routing_policy_comparison.png", 5.0);
    }

    /**
     * Helper to find the TimeSerieCollector in the decorator chain.
     *
     * @param s The simulator instance.
     * @return The TimeSerieCollector if found, null otherwise.
     */
    private TimeSerieCollector findCollector(Simulator s) {
        if (s instanceof TimeSerieCollector c) return c;
        if (s instanceof SimulatorDecorator sd) return findCollector(sd.getDecorated());
        return null;
    }
}

