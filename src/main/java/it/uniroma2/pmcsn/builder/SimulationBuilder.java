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
import it.uniroma2.pmcsn.model.load.routing.webserver.LeastLoadedRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.RoundRobinRoutingStrategy;
import it.uniroma2.pmcsn.model.load.routing.webserver.WebServerRoutingStrategy;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.HorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.horizontal.MovingWindowHorizontalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.UtilizationThresholdVerticalScaler;
import it.uniroma2.pmcsn.model.load.scaler.vertical.VerticalScaler;
import it.uniroma2.pmcsn.model.server.SpikeServer;
import it.uniroma2.pmcsn.model.server.WebServer;
import it.uniroma2.pmcsn.model.server.WebServerCluster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for configuring and creating a SimulationController.
 * Simplifies configuring the simulation with default values and custom overrides.
 */
public class SimulationBuilder {
    private double maxTime = 1000.0;
    private EventSource eventSource;
    private HorizontalScaler scaler;
    private VerticalScaler verticalScaler;
    private SpikeServer spikeServer;
    private final List<WebServer> webServers = new ArrayList<>();
    private RoutingPolicy routingPolicy = RoutingPolicy.ROUND_ROBIN;

    // Default configuration values
    private long seed = 123456789L;
    private double meanInterarrival = 2.0;
    private double meanService = 1.0;
    private int siMax = 5;
    private int webServerCapacity = 1;
    private int webServerCount = 3;
    private int spikeServerCapacity = 10;
    private double spikeCpuPercentage = 0.4;
    private WorkloadType workloadType = WorkloadType.HYPEREXPONENTIAL;

    // Scaling configurations
    private double scaleUpLimit = 8.0;
    private double scaleDownLimit = 2.0;
    private double scaleInterval = 30.0;
    private double cooldown = 30.0;
    private int minServers = 1;
    private int maxServers = 10;
    private double spikeUpperThreshold = 0.70;
    private double spikeLowerThreshold = 0.30;

    /**
     * Applies configuration settings from an ApplicationConfig object.
     */
    public SimulationBuilder config(ApplicationConfig config) {
        this.maxTime = config.maxTime();
        this.seed = config.seed();
        this.meanInterarrival = config.meanInterarrival();
        this.meanService = config.meanService();
        this.siMax = config.siMax();
        this.webServerCount = config.webServersCount();
        this.webServerCapacity = config.webServerCapacity();
        this.spikeServerCapacity = config.spikeServerCapacity();
        this.routingPolicy = config.routingPolicy();
        this.spikeCpuPercentage = config.spikeCpuPercentage();
        this.workloadType = config.workloadType();
        
        // Scaling config
        this.scaleUpLimit = config.scaleUpLimit();
        this.scaleDownLimit = config.scaleDownLimit();
        this.scaleInterval = config.scaleInterval();
        this.cooldown = config.cooldown();
        this.minServers = config.minServers();
        this.maxServers = config.maxServers();
        
        // Vertical scaling config
        this.spikeUpperThreshold = config.spikeUpperThreshold();
        this.spikeLowerThreshold = config.spikeLowerThreshold();

        try {
            if (workloadType == WorkloadType.TRACE && config.tracePath() != null) {
                this.traceFile(config.tracePath());
            } else if (workloadType == WorkloadType.HYPEREXPONENTIAL) {
                this.eventSource = new HyperexponentialEventSource(seed);
            } else {
                this.eventSource = new ExponentialEventSource(seed, meanInterarrival, meanService);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load trace file from path: " + config.tracePath(), e);
        }
        return this;
    }

    public SimulationBuilder maxTime(double maxTime) {
        this.maxTime = maxTime;
        return this;
    }

    public SimulationBuilder seed(long seed) {
        this.seed = seed;
        return this;
    }

    public SimulationBuilder meanInterarrival(double meanInterarrival) {
        this.meanInterarrival = meanInterarrival;
        return this;
    }

    public SimulationBuilder meanService(double meanService) {
        this.meanService = meanService;
        return this;
    }

    public SimulationBuilder siMax(int siMax) {
        this.siMax = siMax;
        return this;
    }

    public SimulationBuilder webServerCapacity(int capacity) {
        this.webServerCapacity = capacity;
        return this;
    }

    public SimulationBuilder webServerCount(int count) {
        this.webServerCount = count;
        return this;
    }

    public SimulationBuilder spikeServerCapacity(int capacity) {
        this.spikeServerCapacity = capacity;
        return this;
    }

    public SimulationBuilder routingPolicy(RoutingPolicy policy) {
        this.routingPolicy = policy;
        return this;
    }

    public SimulationBuilder eventSource(EventSource source) {
        this.eventSource = source;
        return this;
    }

    public SimulationBuilder scaleUpLimit(double limit) {
        this.scaleUpLimit = limit;
        return this;
    }

    public SimulationBuilder scaleDownLimit(double limit) {
        this.scaleDownLimit = limit;
        return this;
    }

    public SimulationBuilder scaleInterval(double interval) {
        this.scaleInterval = interval;
        return this;
    }

    public SimulationBuilder cooldown(double cooldown) {
        this.cooldown = cooldown;
        return this;
    }

    public SimulationBuilder minServers(int min) {
        this.minServers = min;
        return this;
    }

    public SimulationBuilder maxServers(int max) {
        this.maxServers = max;
        return this;
    }

    public SimulationBuilder spikeUpperThreshold(double threshold) {
        this.spikeUpperThreshold = threshold;
        return this;
    }

    public SimulationBuilder spikeLowerThreshold(double threshold) {
        this.spikeLowerThreshold = threshold;
        return this;
    }

    /**
     * Configures the simulation to read from a trace file.
     */
    public SimulationBuilder traceFile(String filePath) throws IOException {
        this.eventSource = new TraceEventSource(filePath);
        return this;
    }

    /**
     * Adds a custom pre-built WebServer.
     */
    public SimulationBuilder addWebServer(WebServer webServer) {
        this.webServers.add(webServer);
        return this;
    }

    /**
     * Sets a custom pre-built HorizontalScaler.
     */
    public SimulationBuilder scaler(HorizontalScaler scaler) {
        this.scaler = scaler;
        return this;
    }

    /**
     * Sets a custom pre-built VerticalScaler.
     */
    public SimulationBuilder verticalScaler(VerticalScaler verticalScaler) {
        this.verticalScaler = verticalScaler;
        return this;
    }

    /**
     * Sets a custom pre-built SpikeServer.
     */
    public SimulationBuilder spikeServer(SpikeServer spikeServer) {
        this.spikeServer = spikeServer;
        return this;
    }

    /**
     * Builds and returns a SimulationController configured with builder options.
     */
    public SimulationController build() {
        if (eventSource == null) {
            if (workloadType == WorkloadType.HYPEREXPONENTIAL) {
                eventSource = new HyperexponentialEventSource(seed);
            } else {
                eventSource = new ExponentialEventSource(seed, meanInterarrival, meanService);
            }
        }

        // WebServerCluster
        WebServerCluster webServerCluster;
        if (!webServers.isEmpty()) {
            webServerCluster = new WebServerCluster(minServers, maxServers, webServerCapacity, webServers);
        } else {
            int finalMin = Math.max(minServers, webServerCount);
            int finalMax = Math.max(maxServers, finalMin);
            webServerCluster = new WebServerCluster(finalMin, finalMax, webServerCapacity);
        }

        // Build Horizontal Scaler if not custom configured
        if (scaler == null) {
            scaler = new MovingWindowHorizontalScaler(scaleUpLimit, scaleDownLimit, scaleInterval, cooldown);
        }

        // Build SpikeServer if not custom configured
        if (spikeServer == null) {
            double speedMultiplier = spikeCpuPercentage / 0.4;
            spikeServer = new SpikeServer(0, spikeServerCapacity, speedMultiplier);
        }

        // Build Vertical Scaler if not custom configured
        if (verticalScaler == null) {
            double baseSpeed = spikeServer.getSpeedMultiplier();
            double scaledSpeed = baseSpeed * 2.0; // Double capacity share on vertical scale up
            verticalScaler = new UtilizationThresholdVerticalScaler(spikeUpperThreshold, spikeLowerThreshold, baseSpeed, scaledSpeed, cooldown);
        }

        // Build Router
        WebServerRoutingStrategy webServerStrategy;
        if (routingPolicy == RoutingPolicy.ROUND_ROBIN) {
            webServerStrategy = new RoundRobinRoutingStrategy();
        } else {
            webServerStrategy = new LeastLoadedRoutingStrategy();
        }
        Router router = new Router(siMax, webServerStrategy);

        // LoadManager
        LoadManager loadController = new LoadManager(scaler, verticalScaler, router);

        return new SimulationController(maxTime, eventSource, webServerCluster, spikeServer, loadController);
    }
}
