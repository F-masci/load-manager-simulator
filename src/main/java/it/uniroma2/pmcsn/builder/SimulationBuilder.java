package it.uniroma2.pmcsn.builder;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.configs.WorkloadType;
import it.uniroma2.pmcsn.controller.SimulationController;
import it.uniroma2.pmcsn.model.event.source.EventSource;
import it.uniroma2.pmcsn.model.event.source.ExponentialEventSource;
import it.uniroma2.pmcsn.model.event.source.HyperexponentialEventSource;
import it.uniroma2.pmcsn.model.event.source.TraceEventSource;
import it.uniroma2.pmcsn.model.load.LoadManager;
import it.uniroma2.pmcsn.model.load.routing.Router;
import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;
import it.uniroma2.pmcsn.model.load.routing.webserver.DeterministicRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.LeastLoadedRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.RoundRobinRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.WebServerRoutingStrategy;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.HorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.MovingWindowHorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.NoHorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.NoVerticalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.UtilizationThresholdVerticalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.VerticalScaler;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;

import java.io.IOException;

/**
 * Fluent builder for configuring and creating a SimulationController.
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
     * Builds and returns a fresh SimulationController using the current configuration.
     * All stateful components are instantiated from scratch.
     */
    public SimulationController build() {
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

        // SpikeServer
        double baseSpeed = config.scaling().spikeCpuPercentage() / 0.4;
        SpikeServer spikeServer = new SpikeServer(0, baseSpeed);

        // Router
        WebServerRoutingStrategy wsStrategy = switch (config.load().routingPolicy()) {
            case RoutingPolicy.ROUND_ROBIN -> new RoundRobinRoutingStrategy();
            case RoutingPolicy.LEAST_LOADED -> new LeastLoadedRoutingStrategy();
            case RoutingPolicy.DETERMINISTIC -> new DeterministicRoutingStrategy();
        };
        Router router = new Router(config.load().siMax(), wsStrategy);

        // Scalers
        HorizontalScaler hScaler = config.scaling().horizontalEnabled()
            ? new MovingWindowHorizontalScaler(config.scaling().scaleUpLimit(), config.scaling().scaleDownLimit(),
                                              config.scaling().scaleInterval(), config.scaling().cooldown())
            : new NoHorizontalScaler();

        double spikeBaseSpeed = spikeServer.getSpeedMultiplier();
        VerticalScaler vScaler = config.scaling().verticalEnabled()
            ? new UtilizationThresholdVerticalScaler(config.scaling().spikeUpperThreshold(), config.scaling().spikeLowerThreshold(),
                                                    spikeBaseSpeed, spikeBaseSpeed * 2.0, config.scaling().cooldown())
            : new NoVerticalScaler();

        // LoadManager
        LoadManager loadManager = new LoadManager(hScaler, vScaler, router);

        return new SimulationController(config.execution().maxTime(), eventSource, cluster, spikeServer, loadManager);
    }
}
