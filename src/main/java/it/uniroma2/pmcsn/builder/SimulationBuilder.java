package it.uniroma2.pmcsn.builder;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.WorkloadType;
import it.uniroma2.pmcsn.controller.SimulationController;
import it.uniroma2.pmcsn.controller.Simulator;
import it.uniroma2.pmcsn.controller.decorator.data.LoadComparisonDecorator;
import it.uniroma2.pmcsn.controller.decorator.data.SystemMetricsDecorator;
import it.uniroma2.pmcsn.controller.decorator.storage.CsvStorageDecorator;
import it.uniroma2.pmcsn.controller.decorator.storage.JsonStorageDecorator;
import it.uniroma2.pmcsn.model.event.source.EventSource;
import it.uniroma2.pmcsn.model.event.source.ExponentialEventSource;
import it.uniroma2.pmcsn.model.event.source.HyperexponentialEventSource;
import it.uniroma2.pmcsn.model.event.source.TraceEventSource;
import it.uniroma2.pmcsn.model.load.LoadManager;
import it.uniroma2.pmcsn.model.load.routing.Router;
import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;
import it.uniroma2.pmcsn.model.load.routing.spike.NoSpikeServerRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.spike.SpikeServerRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.spike.ThresholdSpikeServerRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.DeterministicRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.LeastLoadedRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.PowerOfTwoChoicesRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.RandomRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.RoundRobinRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.WebServerRoutingStrategy;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.HorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.MovingWindowHorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.NoHorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.LoadThresholdVerticalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.NoVerticalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.VerticalScaler;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;

import java.io.IOException;

/**
 * Fluent builder for configuring and creating a Simulator.
 * Relies strictly on ApplicationConfig to ensure that every build() call 
 * creates fresh, independent instances of all simulation components.
 */
public class SimulationBuilder {
    private ApplicationConfig config;

    /**
     * Initializes the builder with default configuration constants.
     */
    public SimulationBuilder() {
        this.config = new ApplicationConfig();
    }

    /**
     * Replaces the entire configuration with a provided ApplicationConfig.
     */
    public SimulationBuilder config(ApplicationConfig config) {
        this.config = config;
        return this;
    }

    /**
     * Overrides the seed for the next build.
     */
    public SimulationBuilder seed(long seed) {
        this.config = config.withSeed(seed);
        return this;
    }

    /**
     * Builds and returns a fresh Simulator using the current configuration.
     * All stateful components are instantiated from scratch.
     */
    public Simulator build() {
        long seed = config.execution().seed();

        // Event Source
        EventSource eventSource;
        try {
            if (config.load().workloadType() == WorkloadType.TRACE && config.load().tracePath() != null) {
                eventSource = new TraceEventSource(config.load().tracePath());
            } else if (config.load().workloadType() == WorkloadType.HYPEREXPONENTIAL) {
                eventSource = new HyperexponentialEventSource(seed, config.load().meanInterarrival(), config.load().meanService());
            } else {
                eventSource = new ExponentialEventSource(seed, config.load().meanInterarrival(), config.load().meanService());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize EventSource", e);
        }

        // WebServerCluster
        WebServerCluster cluster = new WebServerCluster(config.cluster().minServers(), config.cluster().maxServers());
        
        WebServerRoutingStrategy wsStrategy = switch (config.load().routingPolicy()) {
            case RoutingPolicy.ROUND_ROBIN -> new RoundRobinRoutingStrategy();
            case RoutingPolicy.LEAST_LOADED -> new LeastLoadedRoutingStrategy();
            case RoutingPolicy.DETERMINISTIC -> new DeterministicRoutingStrategy();
            case RoutingPolicy.RANDOM -> new RandomRoutingStrategy(seed);
            case RoutingPolicy.POWER_OF_TWO -> new PowerOfTwoChoicesRoutingStrategy(seed);
        };

        // SpikeServer
        SpikeServer spikeServer = new SpikeServer(0, config.scaling().spikeCpuPercentage());
        SpikeServerRoutingStrategy spikeStrategy = config.cluster().spikeEnabled()
                ? new ThresholdSpikeServerRoutingStrategy()
                : new NoSpikeServerRoutingStrategy();

        // Router
        Router router = new Router(config.load().siMax(), wsStrategy, spikeStrategy);

        // Scalers
        HorizontalScaler hScaler = config.scaling().horizontalEnabled()
            ? new MovingWindowHorizontalScaler(config)
            : new NoHorizontalScaler();

        VerticalScaler vScaler = config.scaling().verticalEnabled()
            ? new LoadThresholdVerticalScaler(config)
            : new NoVerticalScaler();

        // LoadManager
        LoadManager loadManager = new LoadManager(hScaler, vScaler, router);

        Simulator controller = new SimulationController(
            config.execution().maxTime(), 
            eventSource, 
            cluster, 
            spikeServer, 
            loadManager,
            config.scaling().scaleInterval()
        );

        // Dynamic decoration
        if (config.logging().enabled()) {
            // First: Data collection decorator (What to save)
            controller = switch (config.logging().dataType()) {
                case LOAD_COMPARISON -> new LoadComparisonDecorator(controller);
                case SYSTEM_METRICS -> new SystemMetricsDecorator(controller);
                case SCALING_METRICS -> new it.uniroma2.pmcsn.controller.decorator.data.ScalingMetricsDecorator(controller);
                case ROUTING_BALANCE -> new it.uniroma2.pmcsn.controller.decorator.data.RoutingBalanceDecorator(controller);
            };

            // Second: Storage decorator (How to save)
            controller = switch (config.logging().format()) {
                case JSON -> new JsonStorageDecorator(controller, config.logging().outputPath());
                case CSV -> new CsvStorageDecorator(controller, config.logging().outputPath());
            };
        }

        return controller;
    }
}
