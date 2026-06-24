package it.uniroma2.pmcsn.utils.chart;

import it.uniroma2.pmcsn.configs.ApplicationConfig;
import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.YIntervalRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Specialized utility for generating charts for simulation objectives.
 * Provides methods for complex grid layouts and statistical visualizations.
 */
public class ObjectiveChartUtility extends BaseChartUtility {

    private static final Color NORD_RED = new Color(191, 97, 106);
    private static final Color NORD_BLUE = new Color(94, 129, 172);
    private static final Color NORD_GREEN = new Color(163, 190, 140);
    private static final Color NORD_ORANGE = new Color(208, 135, 112);
    private static final Color NORD_PURPLE = new Color(180, 142, 173);
    private static final Color NORD_YELLOW = new Color(235, 203, 139);
    private static final Color NORD_CYAN = new Color(136, 192, 208);
    private static final Color NORD_TEAL = new Color(143, 188, 187);

    private static final Color[] PALETTE = { NORD_BLUE, NORD_RED, NORD_GREEN, NORD_ORANGE, NORD_PURPLE, NORD_YELLOW, NORD_CYAN, NORD_TEAL };
    private static final BasicStroke MAIN_STROKE = new BasicStroke(1.8f);
    private static final BasicStroke THIN_STROKE = new BasicStroke(0.9f);

    private static final List<BurstinessMetric> BURSTINESS_METRICS = List.of(
            new BurstinessMetric("response_time", "Tempo di risposta del sistema", "Tempo di risposta R0 [s]", true),
            new BurstinessMetric("jobs_in_system", "Job nel sistema", "Job nel sistema", false),
            new BurstinessMetric("system_utilization", "Utilizzazione del sistema", "Utilizzazione del sistema [%]", false),
            new BurstinessMetric("throughput", "Throughput del sistema", "Throughput [job/s]", false),
            new BurstinessMetric("diverted_jobs", "Job dirottati", "Job dirottati", false),
            new BurstinessMetric("avg_servers", "Server attivi", "Web Server attivi medi", false),
            new BurstinessMetric("scale_out_actions", "Azioni di scale out", "Azioni di scale out", false),
            new BurstinessMetric("scale_in_actions", "Azioni di scale in", "Azioni di scale in", false),
            new BurstinessMetric("scale_up_actions", "Azioni di speed up", "Azioni di speed up", false),
            new BurstinessMetric("scale_down_actions", "Azioni di speed down", "Azioni di speed down", false),
            new BurstinessMetric("spike_avg_speed", "Velocita media dello Spike Server", "Velocita media Spike Server", false),
            new BurstinessMetric("spike_utilization", "Utilizzazione dello Spike Server", "Utilizzazione Spike Server [%]", false)
    );

    /**
     * Regenerates objective charts from existing CSV outputs without re-running simulations.
     *
     * Usage:
     * <pre>
     *   java ... ObjectiveChartUtility all
     *   java ... ObjectiveChartUtility horizontal
     *   java ... ObjectiveChartUtility horizontal input.csv output.png
     * </pre>
     *
     * @param args chart name and optional input/output paths
     * @throws IOException if CSV reading fails
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            logger.info("Usage: ObjectiveChartUtility <all|horizontal|simax|vertical-step|routing|burstiness|cost|finite|vertical-stability> [inputCsv] [outputPng]");
            return;
        }

        String chartName = args[0].toLowerCase();
        switch (chartName) {
            case "all" -> regenerateAllCharts();
            case "horizontal", "horizontal-parameter", "horizontal-scaling" -> {
                String inputCsv = args.length > 1 ? args[1] : "data/objective/horizontal_parameter_estimation.csv";
                String outputPng = args.length > 2 ? args[2] : "data/objective/horizontal_parameter_estimation.png";
                regenerateHorizontalParameterChart(inputCsv, outputPng);
            }
            case "simax" -> regenerateSiMaxChart("data/objective/simax_estimation.csv", "data/objective/simax_estimation.png");
            case "vertical-step", "vertical" -> regenerateVerticalStepChart("data/objective/vertical_step_sizing.csv", "data/objective/vertical_step_sizing.png");
            case "routing" -> regenerateRoutingChart("data/objective/routing_policy_comparison.csv", "data/objective/routing_policy_comparison.png");
            case "burstiness" -> regenerateBurstinessCharts(defaultBurstinessCsvPath(), "data/objective/burstiness/burstiness_robustness.png");
            case "cost" -> regenerateCostChart(defaultCostCsvPath(), "data/objective/cost/cost_analysis.png");
            case "finite", "transient" -> regenerateFiniteCharts("data/objective/finite");
            case "vertical-stability", "stability" -> regenerateVerticalStabilityChart(defaultBandCsvPath(), "data/objective/band/vertical_scaler_stability.png");
            default -> logger.info("Unknown chart '{}'. Available charts: all, horizontal, simax, vertical-step, routing, burstiness, cost, finite, vertical-stability", args[0]);
        }
    }

    private static void regenerateAllCharts() throws IOException {
        regenerateIfPresent("data/objective/simax_estimation.csv", () -> regenerateSiMaxChart("data/objective/simax_estimation.csv", "data/objective/simax_estimation.png"));
        regenerateIfPresent("data/objective/vertical_step_sizing.csv", () -> regenerateVerticalStepChart("data/objective/vertical_step_sizing.csv", "data/objective/vertical_step_sizing.png"));
        regenerateIfPresent("data/objective/horizontal_parameter_estimation.csv", () -> regenerateHorizontalParameterChart("data/objective/horizontal_parameter_estimation.csv", "data/objective/horizontal_parameter_estimation.png"));
        regenerateIfPresent("data/objective/routing_policy_comparison.csv", () -> regenerateRoutingChart("data/objective/routing_policy_comparison.csv", "data/objective/routing_policy_comparison.png"));
        regenerateIfPresent(defaultBurstinessCsvPath(), () -> regenerateBurstinessCharts(defaultBurstinessCsvPath(), "data/objective/burstiness/burstiness_robustness.png"));
        regenerateIfPresent(defaultCostCsvPath(), () -> regenerateCostChart(defaultCostCsvPath(), "data/objective/cost/cost_analysis.png"));
        regenerateIfPresent(defaultBandCsvPath(), () -> regenerateVerticalStabilityChart(defaultBandCsvPath(), "data/objective/band/vertical_scaler_stability.png"));
        regenerateFiniteCharts("data/objective/finite");
    }

    private static void regenerateIfPresent(String csvPath, ChartRegenerator regenerator) throws IOException {
        if (Files.exists(Path.of(csvPath))) {
            regenerator.regenerate();
        } else {
            logger.info("Skipping missing CSV: {}", csvPath);
        }
    }

    private static void regenerateSiMaxChart(String inputCsv, String outputPng) throws IOException {
        XYSeries rtMean = new XYSeries("R0 medio");
        XYSeries rtLower = new XYSeries("Limite inferiore CI 95%");
        XYSeries rtUpper = new XYSeries("Limite superiore CI 95%");
        XYSeries spikeCount = new XYSeries("Job dirottati");
        XYSeries spikeUtil = new XYSeries("Utilizzazione Spike Server [%]");

        for (String line : dataLines(inputCsv)) {
            String[] raw = line.split(",");
            SiMaxCsvRow row = parseSiMaxCsvRow(raw);
            rtMean.add(row.siMax(), row.r0Mean());
            rtLower.add(row.siMax(), row.r0Lower());
            rtUpper.add(row.siMax(), row.r0Upper());
            spikeCount.add(row.siMax(), row.spikeJobsCount());
            spikeUtil.add(row.siMax(), row.spikeUtilizationPct());
        }

        generateSiMaxEstimationStackedChart(rtMean, rtLower, rtUpper, spikeCount, spikeUtil, outputPng, ApplicationConfig.SLA_THRESHOLD);
    }

    private static SiMaxCsvRow parseSiMaxCsvRow(String[] raw) {
        if (raw.length == 7) {
            return new SiMaxCsvRow(
                    Integer.parseInt(raw[0].trim()),
                    Double.parseDouble(raw[1].trim()),
                    Double.parseDouble(raw[2].trim()),
                    Double.parseDouble(raw[3].trim()),
                    Double.parseDouble(raw[4].trim()),
                    Double.parseDouble(raw[5].trim()),
                    Double.parseDouble(raw[6].trim())
            );
        }

        if (raw.length >= 13) {
            return new SiMaxCsvRow(
                    Integer.parseInt(raw[0].trim()),
                    commaDecimal(raw, 1),
                    commaDecimal(raw, 3),
                    commaDecimal(raw, 5),
                    commaDecimal(raw, 7),
                    commaDecimal(raw, 9),
                    commaDecimal(raw, 11)
            );
        }

        throw new IllegalArgumentException("Unsupported siMax CSV row format with " + raw.length + " columns.");
    }

    private static void regenerateVerticalStepChart(String inputCsv, String outputPng) throws IOException {
        XYSeries rtSeries = new XYSeries("Tempo di risposta R0");
        XYSeries speedSeries = new XYSeries("Moltiplicatore medio");
        XYSeries utilSeries = new XYSeries("Utilizzazione [%]");

        for (String line : dataLines(inputCsv)) {
            String[] raw = line.split(",");
            VerticalStepCsvRow row = parseVerticalStepCsvRow(raw);
            rtSeries.add(row.increment(), row.r0());
            speedSeries.add(row.increment(), row.spikeAvgSpeed());
            utilSeries.add(row.increment(), row.spikeUtilizationPct());
        }

        generateVerticalSizingStackedChart(rtSeries, speedSeries, utilSeries, outputPng, ApplicationConfig.SLA_THRESHOLD);
    }

    private static VerticalStepCsvRow parseVerticalStepCsvRow(String[] raw) {
        if (raw.length >= 14) {
            return new VerticalStepCsvRow(
                    commaDecimal(raw, 0),
                    commaDecimal(raw, 2),
                    commaDecimal(raw, 6),
                    commaDecimal(raw, 10)
            );
        }

        if (raw.length == 7) {
            return new VerticalStepCsvRow(
                    Double.parseDouble(raw[0].trim()),
                    Double.parseDouble(raw[1].trim()),
                    Double.parseDouble(raw[3].trim()),
                    Double.parseDouble(raw[5].trim())
            );
        }

        throw new IllegalArgumentException(
                "vertical_step_sizing.csv does not contain Spike_Utilization columns. Re-run VerticalStepSizingObjective to regenerate the CSV."
        );
    }

    private static void regenerateRoutingChart(String inputCsv, String outputPng) throws IOException {
        Map<String, Double> r0Map = new LinkedHashMap<>();
        Map<String, Double> stdDevMap = new LinkedHashMap<>();

        for (String line : dataLines(inputCsv)) {
            String[] raw = line.split(",");
            r0Map.put(formatPolicyName(raw[0].trim()), Double.parseDouble(raw[1].trim()));
            stdDevMap.put(formatPolicyName(raw[0].trim()), Double.parseDouble(raw[3].trim()));
        }

        generateRoutingStatisticalChart(r0Map, stdDevMap, outputPng);
    }

    public static void regenerateBurstinessCharts(String inputCsv, String outputPng) throws IOException {
        Files.createDirectories(Path.of(outputPng).getParent());
        List<String> lines = Files.readAllLines(Path.of(inputCsv));
        if (lines.size() <= 1) {
            logger.info("Skipping empty burstiness CSV: {}", inputCsv);
            return;
        }

        String[] headers = lines.get(0).split(",");
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            headerIndex.put(headers[i].trim(), i);
        }

        if (!headerIndex.containsKey("response_time_std_dev")) {
            regenerateLegacyBurstinessChart(lines, outputPng);
            return;
        }

        for (BurstinessMetric metric : BURSTINESS_METRICS) {
            BurstinessDatasets datasets = readBurstinessDatasets(lines, headerIndex, metric.csvPrefix());
            generateBurstinessMetricChart(
                    datasets.mean(),
                    datasets.lower(),
                    datasets.upper(),
                    datasets.stdDev(),
                    metric.title(),
                    metric.axisLabel(),
                    metricOutputPath(outputPng, metric.csvPrefix()),
                    metric.slaBounded() ? ApplicationConfig.SLA_THRESHOLD : null
            );
        }
    }

    private static void regenerateLegacyBurstinessChart(List<String> lines, String outputPng) {
        Map<Double, XYSeries> meanByCv = new LinkedHashMap<>();
        for (String line : lines.stream().skip(1).filter(l -> !l.isBlank()).toList()) {
            String[] raw = line.split(",");
            double cv = commaDecimal(raw, 0);
            double lambda = commaDecimal(raw, 2);
            double mean = commaDecimal(raw, 4);
            meanByCv.computeIfAbsent(cv, ignored -> new XYSeries("CV=" + cv)).add(lambda, mean);
        }

        XYSeriesCollection meanDataset = new XYSeriesCollection();
        XYSeriesCollection lowerDataset = new XYSeriesCollection();
        XYSeriesCollection upperDataset = new XYSeriesCollection();
        XYSeriesCollection stdDevDataset = new XYSeriesCollection();
        for (Map.Entry<Double, XYSeries> entry : meanByCv.entrySet()) {
            XYSeries source = entry.getValue();
            XYSeries lower = new XYSeries("CV=" + entry.getKey() + " limite inferiore CI 95%");
            XYSeries upper = new XYSeries("CV=" + entry.getKey() + " limite superiore CI 95%");
            XYSeries stdDev = new XYSeries("CV=" + entry.getKey() + " deviazione standard");
            for (int i = 0; i < source.getItemCount(); i++) {
                lower.add(source.getX(i), source.getY(i));
                upper.add(source.getX(i), source.getY(i));
                stdDev.add(source.getX(i), 0.0);
            }
            meanDataset.addSeries(source);
            lowerDataset.addSeries(lower);
            upperDataset.addSeries(upper);
            stdDevDataset.addSeries(stdDev);
        }
        generateBurstinessMetricChart(meanDataset, lowerDataset, upperDataset, stdDevDataset,
                "Tempo di risposta e burstiness", "Tempo di risposta R0 [s]", outputPng, ApplicationConfig.SLA_THRESHOLD);
    }

    private static BurstinessDatasets readBurstinessDatasets(List<String> lines, Map<String, Integer> headerIndex, String metricPrefix) {
        Map<Double, XYSeries> meanByCv = new LinkedHashMap<>();
        Map<Double, XYSeries> lowerByCv = new LinkedHashMap<>();
        Map<Double, XYSeries> upperByCv = new LinkedHashMap<>();
        Map<Double, XYSeries> stdDevByCv = new LinkedHashMap<>();

        int meanIndex = headerIndex.get(metricPrefix + "_mean");
        int stdDevIndex = headerIndex.get(metricPrefix + "_std_dev");
        int lowIndex = headerIndex.get(metricPrefix + "_ci_low");
        int highIndex = headerIndex.get(metricPrefix + "_ci_high");
        double scale = metricPrefix.endsWith("utilization") ? 100.0 : 1.0;

        for (String line : lines.stream().skip(1).filter(l -> !l.isBlank()).toList()) {
            String[] raw = line.split(",");
            double cv = Double.parseDouble(raw[0].trim());
            double lambda = Double.parseDouble(raw[1].trim());
            double mean = Double.parseDouble(raw[meanIndex].trim()) * scale;
            double stdDev = Double.parseDouble(raw[stdDevIndex].trim()) * scale;
            double lower = Double.parseDouble(raw[lowIndex].trim()) * scale;
            double upper = Double.parseDouble(raw[highIndex].trim()) * scale;
            meanByCv.computeIfAbsent(cv, ignored -> new XYSeries("CV=" + cv)).add(lambda, mean);
            lowerByCv.computeIfAbsent(cv, ignored -> new XYSeries("CV=" + cv + " limite inferiore CI 95%")).add(lambda, lower);
            upperByCv.computeIfAbsent(cv, ignored -> new XYSeries("CV=" + cv + " limite superiore CI 95%")).add(lambda, upper);
            stdDevByCv.computeIfAbsent(cv, ignored -> new XYSeries("CV=" + cv + " deviazione standard")).add(lambda, stdDev);
        }

        return new BurstinessDatasets(
                toCollection(meanByCv),
                toCollection(lowerByCv),
                toCollection(upperByCv),
                toCollection(stdDevByCv)
        );
    }

    private static XYSeriesCollection toCollection(Map<Double, XYSeries> seriesByCv) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        seriesByCv.values().forEach(dataset::addSeries);
        return dataset;
    }

    private static String defaultBurstinessCsvPath() {
        String preferred = "data/objective/burstiness/burstiness_robustness.csv";
        if (Files.exists(Path.of(preferred))) {
            return preferred;
        }
        return "data/objective/burstiness_robustness.csv";
    }

    private static String metricOutputPath(String outputPath, String metricName) {
        int extensionIndex = outputPath.lastIndexOf('.');
        String base = extensionIndex >= 0 ? outputPath.substring(0, extensionIndex) : outputPath;
        String extension = extensionIndex >= 0 ? outputPath.substring(extensionIndex) : ".png";
        return base + "_" + metricName + extension;
    }

    private static void regenerateCostChart(String inputCsv, String outputPng) throws IOException {
        Files.createDirectories(Path.of(outputPng).getParent());
        Map<Double, XYSeriesCollection> costData = new LinkedHashMap<>();
        Map<Double, XYSeriesCollection> rtData = new LinkedHashMap<>();
        Map<String, XYSeries> costSeries = new LinkedHashMap<>();
        Map<String, XYSeries> rtSeries = new LinkedHashMap<>();

        for (String line : dataLines(inputCsv)) {
            CostCsvRow row = parseCostCsvRow(line);
            double scaleIn = row.scaleIn();
            double cooldown = row.cooldown();
            double lambda = row.lambda();
            double cost = row.cost();
            double r0 = row.r0();
            String label = "Scale in " + scaleIn + " s";
            String key = cooldown + "|" + label;

            costData.computeIfAbsent(cooldown, ignored -> new XYSeriesCollection());
            rtData.computeIfAbsent(cooldown, ignored -> new XYSeriesCollection());
            XYSeries cSeries = costSeries.computeIfAbsent(key, ignored -> {
                XYSeries s = new XYSeries(label);
                costData.get(cooldown).addSeries(s);
                return s;
            });
            XYSeries rSeries = rtSeries.computeIfAbsent(key, ignored -> {
                XYSeries s = new XYSeries(label);
                rtData.get(cooldown).addSeries(s);
                return s;
            });
            cSeries.add(lambda, cost);
            rSeries.add(lambda, r0);
        }

        generateCostCooldownCharts(costData, rtData, outputPng, ApplicationConfig.SLA_THRESHOLD);
    }

    private static void regenerateFiniteCharts(String finiteDirectory) throws IOException {
        Path dir = Path.of(finiteDirectory);
        if (!Files.isDirectory(dir)) {
            logger.info("Skipping missing finite directory: {}", finiteDirectory);
            return;
        }

        try (var files = Files.list(dir)) {
            for (Path csv : files
                    .filter(path -> path.getFileName().toString().startsWith("transient_"))
                    .filter(path -> path.getFileName().toString().endsWith(".csv"))
                    .sorted()
                    .toList()) {
                XYSeriesCollection dataset = readTransientCsv(csv);
                String baseName = csv.getFileName().toString().replaceFirst("\\.csv$", "");
                String outputPng = csv.resolveSibling(baseName + ".png").toString();
                generateTransientChart(dataset, transientTitleFromFile(baseName), outputPng, ApplicationConfig.SLA_THRESHOLD);
            }
        }
    }

    private static XYSeriesCollection readTransientCsv(Path inputCsv) throws IOException {
        Map<Integer, XYSeries> seriesByReplication = new LinkedHashMap<>();
        for (String line : dataLines(inputCsv.toString())) {
            String[] raw = line.split(",");
            int replication = Integer.parseInt(raw[2].trim());
            long seed = Long.parseLong(raw[3].trim());
            double time = Double.parseDouble(raw[0].trim());
            double r0 = Double.parseDouble(raw[4].trim());
            seriesByReplication
                    .computeIfAbsent(replication, ignored -> new XYSeries("Rep " + replication + " (Seed: " + seed + ")", false, true))
                    .add(time, r0);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        seriesByReplication.values().forEach(dataset::addSeries);
        return dataset;
    }

    private static String transientTitleFromFile(String baseName) {
        String scenario = baseName.replace("transient_", "");
        String load;
        double lambda;
        if (scenario.startsWith("low_load")) {
            load = "basso carico";
            lambda = 2.5;
        } else if (scenario.startsWith("medium_load")) {
            load = "carico medio";
            lambda = 5.0;
        } else if (scenario.startsWith("high_load")) {
            load = "alto carico";
            lambda = 8.0;
        } else {
            load = scenario.replace("_", " ");
            lambda = Double.NaN;
        }

        String cv = scenario.contains("cv") ? scenario.substring(scenario.lastIndexOf("cv") + 2) : "";
        String cvText = cv.isBlank() ? "" : " CV " + cv;
        String lambdaText = Double.isNaN(lambda) ? "" : " lambda " + String.format(Locale.US, "%.1f", lambda);
        return "Analisi transitoria " + load + cvText + lambdaText;
    }

    private static CostCsvRow parseCostCsvRow(String line) {
        String[] raw = line.split(",");
        if (raw.length == 8) {
            return new CostCsvRow(
                    Double.parseDouble(raw[0].trim()),
                    Double.parseDouble(raw[1].trim()),
                    Double.parseDouble(raw[2].trim()),
                    Double.parseDouble(raw[6].trim()),
                    Double.parseDouble(raw[7].trim())
            );
        }

        return new CostCsvRow(
                commaDecimal(raw, 0),
                commaDecimal(raw, 2),
                commaDecimal(raw, 4),
                commaDecimal(raw, 12),
                commaDecimal(raw, 14)
        );
    }

    private static String defaultCostCsvPath() {
        String preferred = "data/objective/cost/cost_analysis.csv";
        if (Files.exists(Path.of(preferred))) {
            return preferred;
        }
        return "data/objective/cost_analysis.csv";
    }

    public static void regenerateVerticalStabilityChart(String inputCsv, String outputPng) throws IOException {
        Files.createDirectories(Path.of(outputPng).getParent());
        List<String> lines = Files.readAllLines(Path.of(inputCsv));
        if (lines.size() <= 1) {
            logger.info("Skipping empty vertical stability CSV: {}", inputCsv);
            return;
        }

        String[] headers = lines.get(0).split(",");
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            headerIndex.put(headers[i].trim(), i);
        }

        if (!headerIndex.containsKey("spike_utilization_mean")) {
            regenerateLegacyVerticalStabilityChart(lines, outputPng);
            return;
        }

        Map<Integer, StabilityDatasets> spikeData = new LinkedHashMap<>();
        Map<Integer, StabilityDatasets> systemData = new LinkedHashMap<>();
        Map<Integer, StabilityDatasets> rtData = new LinkedHashMap<>();
        Map<String, StabilitySeries> spikeSeries = new LinkedHashMap<>();
        Map<String, StabilitySeries> systemSeries = new LinkedHashMap<>();
        Map<String, StabilitySeries> rtSeries = new LinkedHashMap<>();

        for (String line : lines.stream().skip(1).filter(l -> !l.isBlank()).toList()) {
            String[] raw = line.split(",");
            int band = Integer.parseInt(raw[headerIndex.get("Band")].trim());
            double cv = Double.parseDouble(raw[headerIndex.get("CV")].trim());
            double lambda = Double.parseDouble(raw[headerIndex.get("Lambda")].trim());

            addStabilityPoint(spikeData, spikeSeries, band, cv, lambda,
                    parseMetric(raw, headerIndex, "spike_utilization", true));
            addStabilityPoint(systemData, systemSeries, band, cv, lambda,
                    parseMetric(raw, headerIndex, "system_utilization", true));
            addStabilityPoint(rtData, rtSeries, band, cv, lambda,
                    parseMetric(raw, headerIndex, "response_time", false));
        }

        generateVerticalScalerStabilityCharts(spikeData, systemData, rtData, outputPng, ApplicationConfig.SLA_THRESHOLD);
    }

    private static void regenerateLegacyVerticalStabilityChart(List<String> lines, String outputPng) {
        Map<Integer, StabilityDatasets> spikeData = new LinkedHashMap<>();
        Map<Integer, StabilityDatasets> rtData = new LinkedHashMap<>();
        Map<String, StabilitySeries> spikeSeries = new LinkedHashMap<>();
        Map<String, StabilitySeries> rtSeries = new LinkedHashMap<>();

        for (String line : lines.stream().skip(1).filter(l -> !l.isBlank()).toList()) {
            VerticalStabilityCsvRow row = parseVerticalStabilityCsvRow(line);
            addStabilityPoint(spikeData, spikeSeries, row.band(), row.cv(), row.lambda(),
                    new MetricPoint(row.utilization() * 100.0, row.utilization() * 100.0, row.utilization() * 100.0, 0.0));
            addStabilityPoint(rtData, rtSeries, row.band(), row.cv(), row.lambda(),
                    new MetricPoint(row.r0(), row.r0(), row.r0(), 0.0));
        }

        generateVerticalScalerStabilityCharts(spikeData, Collections.emptyMap(), rtData, outputPng, ApplicationConfig.SLA_THRESHOLD);
    }

    private static MetricPoint parseMetric(String[] raw, Map<String, Integer> headerIndex, String prefix, boolean percentage) {
        double scale = percentage ? 100.0 : 1.0;
        double mean = Double.parseDouble(raw[headerIndex.get(prefix + "_mean")].trim()) * scale;
        double lower = Double.parseDouble(raw[headerIndex.get(prefix + "_ci_low")].trim()) * scale;
        double upper = Double.parseDouble(raw[headerIndex.get(prefix + "_ci_high")].trim()) * scale;
        double stdDev = Double.parseDouble(raw[headerIndex.get(prefix + "_std_dev")].trim()) * scale;
        return new MetricPoint(mean, lower, upper, stdDev);
    }

    private static void addStabilityPoint(
            Map<Integer, StabilityDatasets> datasetsByBand,
            Map<String, StabilitySeries> seriesByKey,
            int band,
            double cv,
            double lambda,
            MetricPoint point) {

        StabilityDatasets datasets = datasetsByBand.computeIfAbsent(band, ignored -> new StabilityDatasets(
                new XYSeriesCollection(),
                new XYSeriesCollection(),
                new XYSeriesCollection(),
                new XYSeriesCollection()
        ));

        String label = "CV=" + cv;
        String key = band + "|" + label;
        StabilitySeries series = seriesByKey.computeIfAbsent(key, ignored -> {
            StabilitySeries created = new StabilitySeries(
                    new XYSeries(label),
                    new XYSeries(label + " limite inferiore CI 95%"),
                    new XYSeries(label + " limite superiore CI 95%"),
                    new XYSeries(label + " deviazione standard")
            );
            datasets.mean().addSeries(created.mean());
            datasets.lower().addSeries(created.lower());
            datasets.upper().addSeries(created.upper());
            datasets.stdDev().addSeries(created.stdDev());
            return created;
        });

        series.mean().add(lambda, point.mean());
        series.lower().add(lambda, point.lower());
        series.upper().add(lambda, point.upper());
        series.stdDev().add(lambda, point.stdDev());
    }

    private static String defaultBandCsvPath() {
        String preferred = "data/objective/band/vertical_scaler_stability.csv";
        if (Files.exists(Path.of(preferred))) {
            return preferred;
        }
        return "data/objective/vertical_scaler_stability.csv";
    }

    private static VerticalStabilityCsvRow parseVerticalStabilityCsvRow(String line) {
        String[] raw = line.split(",");
        if (raw.length == 5) {
            return new VerticalStabilityCsvRow(
                    Integer.parseInt(raw[0].trim()),
                    Double.parseDouble(raw[1].trim()),
                    Double.parseDouble(raw[2].trim()),
                    Double.parseDouble(raw[3].trim()),
                    Double.parseDouble(raw[4].trim())
            );
        }

        return new VerticalStabilityCsvRow(
                Integer.parseInt(raw[0].trim()),
                commaDecimal(raw, 1),
                commaDecimal(raw, 3),
                commaDecimal(raw, 5),
                commaDecimal(raw, 7)
        );
    }

    private static void regenerateHorizontalParameterChart(String inputCsv, String outputPng) throws IOException {
        Map<Double, Map<Integer, XYSeries>> cvResults = readHorizontalParameterCsv(Path.of(inputCsv));
        generateHorizontalParameterEstimationChart(cvResults, outputPng, ApplicationConfig.SLA_THRESHOLD);
    }

    private static Map<Double, Map<Integer, XYSeries>> readHorizontalParameterCsv(Path inputCsv) throws IOException {
        Map<Double, Map<Integer, XYSeries>> cvResults = new LinkedHashMap<>();

        for (String line : Files.readAllLines(inputCsv).stream().skip(1).toList()) {
            if (line.isBlank()) {
                continue;
            }

            HorizontalCsvRow row = parseHorizontalCsvRow(line);
            Map<Integer, XYSeries> serverSeries = cvResults.computeIfAbsent(row.cv(), ignored -> new LinkedHashMap<>());
            int seriesKey = seriesKey(row);
            XYSeries series = serverSeries.computeIfAbsent(seriesKey, ignored -> new XYSeries(seriesName(row), false, true));

            double y = "CONVERGED".equals(row.status()) ? row.r0() : ApplicationConfig.SLA_THRESHOLD * 6.0;
            series.add(row.lambda(), y);
        }

        return cvResults;
    }

    private static HorizontalCsvRow parseHorizontalCsvRow(String line) {
        String[] raw = line.split(",");
        int statusIndex = findStatusIndex(raw);

        double cv;
        double lambda;
        int servers;
        double r0;

        if (raw.length >= 9 && raw[0].contains(".") && raw[1].contains(".")) {
            cv = Double.parseDouble(raw[0].trim());
            lambda = Double.parseDouble(raw[1].trim());
            servers = Integer.parseInt(raw[2].trim());
            r0 = parseOptionalDouble(raw[3].trim());
        } else {
            cv = decimal(raw, 0);
            lambda = decimal(raw, 2);
            servers = Integer.parseInt(raw[4].trim());
            r0 = parseOptionalDecimal(raw, 5);
        }
        String status = raw[statusIndex].trim();

        return new HorizontalCsvRow(cv, lambda, servers, r0, status);
    }

    private static int findStatusIndex(String[] raw) {
        for (int i = 0; i < raw.length; i++) {
            String token = raw[i].trim();
            if ("CONVERGED".equals(token)
                    || "DIVERGING".equals(token)
                    || "INCONCLUSIVE".equals(token)
                    || token.startsWith("SKIPPED_AFTER_")) {
                return i;
            }
        }
        throw new IllegalArgumentException("Missing convergence status in horizontal CSV row.");
    }

    private static double decimal(String[] raw, int integerIndex) {
        String token = raw[integerIndex].trim();
        if ("NaN".equals(token) || token.contains(".")) {
            return Double.parseDouble(token);
        }
        return Double.parseDouble(raw[integerIndex].trim() + "." + raw[integerIndex + 1].trim());
    }

    private static double parseOptionalDecimal(String[] raw, int integerIndex) {
        if ("NaN".equals(raw[integerIndex].trim())) {
            return Double.NaN;
        }
        return decimal(raw, integerIndex);
    }

    private static double parseOptionalDouble(String token) {
        return "NaN".equals(token) ? Double.NaN : Double.parseDouble(token);
    }

    private static List<String> dataLines(String inputCsv) throws IOException {
        return Files.readAllLines(Path.of(inputCsv)).stream()
                .skip(1)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private static double commaDecimal(String[] raw, int integerIndex) {
        return Double.parseDouble(raw[integerIndex].trim() + "." + raw[integerIndex + 1].trim());
    }

    private static String formatPolicyName(String policy) {
        return switch (policy) {
            case "ROUND_ROBIN" -> "Round Robin";
            case "LEAST_LOADED" -> "Least Loaded";
            case "POWER_OF_TWO" -> "Power of Two Choices";
            case "RANDOM" -> "Random";
            default -> policy;
        };
    }

    @FunctionalInterface
    private interface ChartRegenerator {
        void regenerate() throws IOException;
    }

    private static int seriesKey(HorizontalCsvRow row) {
        if ("CONVERGED".equals(row.status())) {
            return row.servers();
        }
        if ("DIVERGING".equals(row.status()) || row.status().startsWith("SKIPPED_AFTER_DIVERGING")) {
            return -row.servers();
        }
        return -100 - row.servers();
    }

    private static String seriesName(HorizontalCsvRow row) {
        if ("CONVERGED".equals(row.status())) {
            return row.servers() + " WS";
        }
        if ("DIVERGING".equals(row.status()) || row.status().startsWith("SKIPPED_AFTER_DIVERGING")) {
            return row.servers() + " WS - DIVERGING";
        }
        return row.servers() + " WS - INCONCLUSIVE";
    }

    private record HorizontalCsvRow(double cv, double lambda, int servers, double r0, String status) { }

    private record CostCsvRow(double scaleIn, double cooldown, double lambda, double cost, double r0) { }

    private record SiMaxCsvRow(
            int siMax,
            double r0Mean,
            double r0Lower,
            double r0Upper,
            double spikeJobsCount,
            double spikeJobsPerc,
            double spikeUtilizationPct
    ) { }

    private record VerticalStepCsvRow(double increment, double r0, double spikeAvgSpeed, double spikeUtilizationPct) { }

    private record VerticalStabilityCsvRow(int band, double cv, double lambda, double utilization, double r0) { }

    private record MetricPoint(double mean, double lower, double upper, double stdDev) { }

    private record StabilitySeries(XYSeries mean, XYSeries lower, XYSeries upper, XYSeries stdDev) { }

    private record StabilityDatasets(
            XYSeriesCollection mean,
            XYSeriesCollection lower,
            XYSeriesCollection upper,
            XYSeriesCollection stdDev
    ) { }

    private record BurstinessMetric(String csvPrefix, String title, String axisLabel, boolean slaBounded) { }

    private record BurstinessDatasets(
            XYSeriesCollection mean,
            XYSeriesCollection lower,
            XYSeriesCollection upper,
            XYSeriesCollection stdDev
    ) { }

    /**
     * Generates a stacked chart for SI_max estimation (Objective 1.1).
     *
     * @param rtMean       Mean response time series.
     * @param rtLower      Lower bound response time series.
     * @param rtUpper      Upper bound response time series.
     * @param spikeCount   Diverted jobs count series.
     * @param spikeUtil    Spike server utilization series.
     * @param outputPath   The path to save the generated image.
     * @param slaThreshold The SLA threshold for response time.
     */
    public static void generateSiMaxEstimationStackedChart(
            XYSeries rtMean, XYSeries rtLower, XYSeries rtUpper,
            XYSeries spikeCount, XYSeries spikeUtil,
            String outputPath, double slaThreshold) {

        XYSeriesCollection rtDataset = new XYSeriesCollection();
        rtDataset.addSeries(rtMean);
        rtDataset.addSeries(rtLower);
        rtDataset.addSeries(rtUpper);

        NumberAxis rtAxis = new NumberAxis("Tempo di risposta R0 [s]");
        XYLineAndShapeRenderer rtRenderer = new XYLineAndShapeRenderer(true, true);
        rtRenderer.setSeriesPaint(0, Color.BLUE);
        rtRenderer.setSeriesStroke(0, new BasicStroke(2.0f));

        Color ciBlue = new Color(94, 129, 172, 210);
        BasicStroke dashed = new BasicStroke(1.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{7, 5}, 0);
        rtRenderer.setSeriesPaint(1, ciBlue);
        rtRenderer.setSeriesStroke(1, dashed);
        rtRenderer.setSeriesShapesVisible(1, false);
        rtRenderer.setSeriesVisibleInLegend(1, true);
        rtRenderer.setSeriesPaint(2, ciBlue);
        rtRenderer.setSeriesStroke(2, dashed);
        rtRenderer.setSeriesShapesVisible(2, false);
        rtRenderer.setSeriesVisibleInLegend(2, true);

        XYPlot rtPlot = new XYPlot(rtDataset, null, rtAxis, rtRenderer);
        setupPlot(rtPlot);
        applyLimit(rtPlot, slaThreshold, "SLA");

        XYSeriesCollection countDataset = new XYSeriesCollection(spikeCount);
        NumberAxis countAxis = new NumberAxis("Job dirottati");
        XYLineAndShapeRenderer countRenderer = new XYLineAndShapeRenderer(true, true);
        countRenderer.setSeriesPaint(0, NORD_RED);

        XYPlot spikePlot = new XYPlot(countDataset, null, countAxis, countRenderer);
        setupPlot(spikePlot);

        NumberAxis utilAxis = new NumberAxis("Utilizzazione Spike Server [%]");
        utilAxis.setRange(0, 100);
        spikePlot.setRangeAxis(1, utilAxis);
        spikePlot.setDataset(1, new XYSeriesCollection(spikeUtil));
        spikePlot.mapDatasetToRangeAxis(1, 1);

        XYLineAndShapeRenderer utilRenderer = new XYLineAndShapeRenderer(true, true);
        utilRenderer.setSeriesPaint(0, NORD_CYAN);
        spikePlot.setRenderer(1, utilRenderer);

        NumberAxis domainAxis = new NumberAxis("Soglia SI max");
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(domainAxis);
        combinedPlot.add(rtPlot, 2);
        combinedPlot.add(spikePlot, 1);
        combinedPlot.setGap(15.0);

        JFreeChart chart = new JFreeChart("Calibrazione soglia SI max", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, true);
        chart.setBackgroundPaint(Color.WHITE);
        saveChart(chart, outputPath, 1000, 800);
    }

    /**
     * Generates a stacked chart for vertical step sizing (Objective 1.2).
     *
     * @param rtSeries     Response time series.
     * @param speedSeries  Average speed multiplier series.
     * @param utilSeries   Utilization series.
     * @param outputPath   The path to save the generated image.
     * @param slaThreshold The SLA threshold for response time.
     */
    public static void generateVerticalSizingStackedChart(
            XYSeries rtSeries, XYSeries speedSeries, XYSeries utilSeries,
            String outputPath, double slaThreshold) {

        XYPlot rtPlot = createLineSubplot(rtSeries, "Tempo di risposta R0 [s]", NORD_BLUE);
        applyLimit(rtPlot, slaThreshold, "SLA");
        XYPlot speedPlot = createLineSubplot(speedSeries, "Moltiplicatore medio", NORD_RED);
        XYPlot utilPlot = createLineSubplot(utilSeries, "Utilizzazione [%]", NORD_TEAL);
        configurePercentageAxis((NumberAxis) utilPlot.getRangeAxis());

        NumberAxis domainAxis = new NumberAxis("Incremento di velocita");
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(domainAxis);
        combinedPlot.add(rtPlot, 1);
        combinedPlot.add(speedPlot, 1);
        combinedPlot.add(utilPlot, 1);
        combinedPlot.setGap(10.0);

        JFreeChart chart = new JFreeChart("Dimensionamento scaling verticale", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, true);
        chart.setBackgroundPaint(Color.WHITE);
        saveChart(chart, outputPath, 1000, 900);
    }


    /**
     * Generates a routing policy chart showing only R0 with StdDev error bars.
     * Used when P99 data is not available (e.g., in Batch Means mode).
     *
     * @param r0Map      Map of policy names to mean response times.
     * @param stdDevMap   Map of policy names to standard deviations.
     * @param outputPath  The path to save the generated image.
     */
    public static void generateRoutingStatisticalChart(
            Map<String, Double> r0Map, Map<String, Double> stdDevMap,
            String outputPath) {

        DefaultStatisticalCategoryDataset barDataset = new DefaultStatisticalCategoryDataset();
        r0Map.keySet().forEach(policy ->
                barDataset.add(r0Map.get(policy), stdDevMap.get(policy), "R0 medio", policy));

        CategoryPlot plot = new CategoryPlot();
        plot.setDomainAxis(new CategoryAxis("Politica di routing"));

        double maxR0 = r0Map.values().stream().max(Double::compareTo).orElse(1.0);
        double maxStdDev = stdDevMap.values().stream().max(Double::compareTo).orElse(0.0);
        NumberAxis rangeAxis = new NumberAxis("Tempo di risposta R0 [s]");
        rangeAxis.setRange(0.0, (maxR0 + maxStdDev) * 1.20);
        plot.setRangeAxis(rangeAxis);

        ClippedColoredStatisticalBarRenderer barRenderer = new ClippedColoredStatisticalBarRenderer();
        barRenderer.setBarPainter(new StandardBarPainter());
        barRenderer.setShadowVisible(false);
        plot.setDataset(0, barDataset);
        plot.setRenderer(0, barRenderer);

        JFreeChart chart = new JFreeChart("Confronto politiche di routing", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(Color.WHITE);
        saveChart(chart, outputPath, 1000, 700);
    }

    /**
     * Generates a routing policy chart with R0, StdDev error bars, P99 line, and SLA threshold.
     *
     * @param r0Map         Map of policy names to mean response times.
     * @param stdDevMap      Map of policy names to standard deviations.
     * @param p99Map         Map of policy names to 99th percentile values.
     * @param outputPath     The path to save the generated image.
     * @param slaThreshold   The SLA threshold for response time.
     */
    public static void generateRoutingStatisticalChart(
            Map<String, Double> r0Map, Map<String, Double> stdDevMap, Map<String, Double> p99Map,
            String outputPath, double slaThreshold) {

        DefaultStatisticalCategoryDataset barDataset = new DefaultStatisticalCategoryDataset();
        DefaultCategoryDataset lineDataset = new DefaultCategoryDataset();
        r0Map.keySet().forEach(policy -> {
            barDataset.add(r0Map.get(policy), stdDevMap.get(policy), "R0 medio", policy);
            lineDataset.addValue(p99Map.get(policy), "Percentile 99", policy);
        });

        CategoryPlot plot = new CategoryPlot();
        plot.setDomainAxis(new CategoryAxis("Politica di routing"));
        double maxP99 = p99Map.values().stream().max(Double::compareTo).orElse(3.0);
        NumberAxis rangeAxis = new NumberAxis("Tempo di risposta R0 [s]");
        rangeAxis.setRange(0.0, Math.max(maxP99, slaThreshold) * 1.20);
        plot.setRangeAxis(rangeAxis);

        ClippedColoredStatisticalBarRenderer barRenderer = new ClippedColoredStatisticalBarRenderer();
        barRenderer.setBarPainter(new StandardBarPainter());
        barRenderer.setShadowVisible(false);
        plot.setDataset(0, barDataset);
        plot.setRenderer(0, barRenderer);

        LineAndShapeRenderer p99Renderer = new LineAndShapeRenderer(true, true);
        p99Renderer.setSeriesPaint(0, NORD_RED);
        p99Renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        p99Renderer.setSeriesShape(0, new Rectangle2D.Double(-4, -4, 8, 8));
        plot.setDataset(1, lineDataset);
        plot.setRenderer(1, p99Renderer);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        applyLimitCategory(plot, slaThreshold, "SLA");

        JFreeChart chart = new JFreeChart("Confronto politiche di routing", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setBackgroundPaint(Color.WHITE);
        saveChart(chart, outputPath, 1000, 700);
    }

    /**
     * Generates one cost/performance chart for each cooldown value.
     *
     * @param costData     Map of cooldowns to cost datasets.
     * @param rtData       Map of cooldowns to response time datasets.
     * @param outputPath   Base path used to derive one output image per cooldown.
     * @param slaThreshold The SLA threshold for response time.
     */
    public static void generateCostCooldownCharts(
            Map<Double, XYSeriesCollection> costData,
            Map<Double, XYSeriesCollection> rtData,
            String outputPath, double slaThreshold) {

        List<Double> sortedCooldowns = new ArrayList<>(costData.keySet());
        Collections.sort(sortedCooldowns);

        for (Double cd : sortedCooldowns) {
            XYLineAndShapeRenderer sharedRenderer = createCustomRenderer();
            NumberAxis domainAxis = new NumberAxis("Tasso di arrivo lambda [job/s]");
            CombinedDomainXYPlot plot = new CombinedDomainXYPlot(domainAxis);
            plot.setGap(12.0);

            XYPlot costPlot = createXYSubplot(costData.get(cd), "Costo totale", sharedRenderer);
            XYPlot rtPlot = createXYSubplot(rtData.get(cd), "Tempo di risposta R0 [s]", sharedRenderer);
            applyLimit(rtPlot, slaThreshold, "SLA");

            plot.add(costPlot, 1);
            plot.add(rtPlot, 1);

            JFreeChart chart = new JFreeChart(
                    "Costo e prestazioni cooldown " + formatCooldown(cd),
                    JFreeChart.DEFAULT_TITLE_FONT,
                    plot,
                    true
            );
            chart.setBackgroundPaint(Color.WHITE);
            configureLegend(chart, costData.get(cd), sharedRenderer);
            saveChart(chart, cooldownOutputPath(outputPath, cd), 1000, 650);
        }
    }

    private static String cooldownOutputPath(String outputPath, double cooldown) {
        int extensionIndex = outputPath.lastIndexOf('.');
        String base = extensionIndex >= 0 ? outputPath.substring(0, extensionIndex) : outputPath;
        String extension = extensionIndex >= 0 ? outputPath.substring(extensionIndex) : ".png";
        return base + "_cooldown_" + formatCooldown(cooldown).replace(".", "_").replace("s", "s") + extension;
    }

    private static String formatCooldown(double cooldown) {
        if (Math.rint(cooldown) == cooldown) {
            return String.format("%.0fs", cooldown);
        }
        return String.format("%.1fs", cooldown);
    }

    /**
     * Generates one vertical scaler stability chart for each hysteresis band using
     * mean, standard-deviation bars and 95% confidence intervals.
     *
     * @param spikeData    Map of bands to spike-utilization datasets.
     * @param systemData   Map of bands to system-utilization datasets.
     * @param rtData       Map of bands to response-time datasets.
     * @param outputPath   Base path used to derive one output image per band.
     * @param slaThreshold The SLA threshold for response time.
     */
    public static void generateVerticalScalerStabilityCharts(
            Map<Integer, StabilityDatasets> spikeData,
            Map<Integer, StabilityDatasets> systemData,
            Map<Integer, StabilityDatasets> rtData,
            String outputPath, double slaThreshold) {

        List<Integer> sortedBands = new ArrayList<>(spikeData.keySet());
        Collections.sort(sortedBands);

        for (Integer band : sortedBands) {
            NumberAxis domainAxis = new NumberAxis("Tasso di arrivo lambda [job/s]");
            CombinedDomainXYPlot plot = new CombinedDomainXYPlot(domainAxis);
            plot.setGap(12.0);

            if (systemData.containsKey(band)) {
                plot.add(createStabilityMetricSubplot(systemData.get(band), "Utilizzazione del sistema [%]", null), 1);
            }
            plot.add(createStabilityMetricSubplot(spikeData.get(band), "Utilizzazione Spike Server [%]", null), 1);
            plot.add(createStabilityMetricSubplot(rtData.get(band), "Tempo di risposta R0 [s]", slaThreshold), 1);

            JFreeChart chart = new JFreeChart(
                    "Stabilita scaling verticale banda " + band,
                    JFreeChart.DEFAULT_TITLE_FONT,
                    plot,
                    true
            );
            chart.setBackgroundPaint(Color.WHITE);
            configureCvLegend(chart, spikeData.get(band).mean());
            saveChart(chart, bandOutputPath(outputPath, band), 1050, systemData.containsKey(band) ? 850 : 650);
        }
    }

    private static XYPlot createStabilityMetricSubplot(StabilityDatasets datasets, String axisLabel, Double slaThreshold) {
        YIntervalSeriesCollection ciDataset = new YIntervalSeriesCollection();
        YIntervalSeriesCollection stdDevIntervalDataset = new YIntervalSeriesCollection();
        XYSeriesCollection ciBoundaryDataset = new XYSeriesCollection();

        for (int s = 0; s < datasets.mean().getSeriesCount(); s++) {
            XYSeries mean = datasets.mean().getSeries(s);
            XYSeries lower = datasets.lower().getSeries(s);
            XYSeries upper = datasets.upper().getSeries(s);
            XYSeries stdDev = datasets.stdDev().getSeries(s);

            YIntervalSeries ciSeries = new YIntervalSeries(mean.getKey());
            YIntervalSeries stdDevSeries = new YIntervalSeries(mean.getKey() + " deviazione standard");
            XYSeries lowerBoundary = new XYSeries(mean.getKey() + " limite inferiore CI 95%");
            XYSeries upperBoundary = new XYSeries(mean.getKey() + " limite superiore CI 95%");

            for (int i = 0; i < mean.getItemCount(); i++) {
                double x = mean.getX(i).doubleValue();
                double y = mean.getY(i).doubleValue();
                double lowerCi = lower.getY(i).doubleValue();
                double upperCi = upper.getY(i).doubleValue();
                double sigma = stdDev.getY(i).doubleValue();
                ciSeries.add(x, y, lowerCi, upperCi);
                stdDevSeries.add(x, y, Math.max(0.0, y - sigma), y + sigma);
                lowerBoundary.add(x, lowerCi);
                upperBoundary.add(x, upperCi);
            }

            ciDataset.addSeries(ciSeries);
            stdDevIntervalDataset.addSeries(stdDevSeries);
            ciBoundaryDataset.addSeries(lowerBoundary);
            ciBoundaryDataset.addSeries(upperBoundary);
        }

        NumberAxis axis = new NumberAxis(axisLabel);
        axis.setAutoRangeIncludesZero(true);
        configurePercentageAxis(axis);

        DeviationRenderer ciRenderer = new DeviationRenderer(true, true);
        ciRenderer.setAlpha(0.16f);
        for (int i = 0; i < ciDataset.getSeriesCount(); i++) {
            Color color = PALETTE[i % PALETTE.length];
            ciRenderer.setSeriesPaint(i, color);
            ciRenderer.setSeriesFillPaint(i, new Color(color.getRed(), color.getGreen(), color.getBlue(), 55));
            ciRenderer.setSeriesStroke(i, new BasicStroke(2.0f));
            ciRenderer.setSeriesShapesVisible(i, true);
        }

        XYPlot plot = new XYPlot(ciDataset, null, axis, ciRenderer);
        setupPlot(plot);

        XYLineAndShapeRenderer boundaryRenderer = new XYLineAndShapeRenderer(true, false);
        BasicStroke dashed = new BasicStroke(1.1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5, 5}, 0);
        for (int i = 0; i < ciBoundaryDataset.getSeriesCount(); i++) {
            Color color = PALETTE[(i / 2) % PALETTE.length];
            boundaryRenderer.setSeriesPaint(i, new Color(color.getRed(), color.getGreen(), color.getBlue(), 145));
            boundaryRenderer.setSeriesStroke(i, dashed);
            boundaryRenderer.setSeriesVisibleInLegend(i, false);
        }
        plot.setDataset(1, ciBoundaryDataset);
        plot.setRenderer(1, boundaryRenderer);

        YIntervalRenderer stdDevRenderer = new YIntervalRenderer();
        for (int i = 0; i < stdDevIntervalDataset.getSeriesCount(); i++) {
            stdDevRenderer.setSeriesPaint(i, PALETTE[i % PALETTE.length]);
            stdDevRenderer.setSeriesStroke(i, new BasicStroke(1.1f));
            stdDevRenderer.setSeriesVisibleInLegend(i, false);
        }
        plot.setDataset(2, stdDevIntervalDataset);
        plot.setRenderer(2, stdDevRenderer);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        if (slaThreshold != null) {
            applyLimit(plot, slaThreshold, "SLA");
        }

        return plot;
    }

    private static String bandOutputPath(String outputPath, int band) {
        int extensionIndex = outputPath.lastIndexOf('.');
        String base = extensionIndex >= 0 ? outputPath.substring(0, extensionIndex) : outputPath;
        String extension = extensionIndex >= 0 ? outputPath.substring(extensionIndex) : ".png";
        return base + "_band_" + band + extension;
    }

    /**
     * Generates a chart for horizontal parameter estimation (Objective 1.3).
     *
     * @param cvResults    Map of CVs to server count series.
     * @param outputPath   The path to save the generated image.
     * @param slaThreshold The SLA threshold for response time.
     */
    public static void generateHorizontalParameterEstimationChart(
            Map<Double, Map<Integer, XYSeries>> cvResults, String outputPath, double slaThreshold) {
        XYPlot mainPlot = new XYPlot();
        mainPlot.setDomainAxis(new NumberAxis("Tasso di arrivo lambda [job/s]"));
        List<Double> sortedCvs = new ArrayList<>(cvResults.keySet());
        Collections.sort(sortedCvs);

        XYSeriesCollection globalDataset = new XYSeriesCollection();
        XYSeries divergingMarkers = new XYSeries("Divergente", false, true);
        XYSeries skippedMarkers = new XYSeries("Saltata o inconclusiva", false, true);
        double maxConvergedR0 = slaThreshold;

        for (Double cv : sortedCvs) {
            Map<Integer, XYSeries> wsSeries = cvResults.get(cv);
            for (XYSeries source : wsSeries.values()) {
                String key = source.getKey().toString();
                boolean diverging = key.contains("DIVERGING");
                boolean inconclusive = key.contains("INCONCLUSIVE");

                if (!diverging && !inconclusive) {
                    globalDataset.addSeries(source);
                    for (int i = 0; i < source.getItemCount(); i++) {
                        maxConvergedR0 = Math.max(maxConvergedR0, source.getY(i).doubleValue());
                    }
                    continue;
                }

                for (int i = 0; i < source.getItemCount(); i++) {
                    double x = source.getX(i).doubleValue();
                    double y = slaThreshold * 1.06;
                    if (diverging) {
                        divergingMarkers.add(x, y);
                    } else {
                        skippedMarkers.add(x, y);
                    }
                }
            }
        }

        globalDataset.addSeries(divergingMarkers);
        globalDataset.addSeries(skippedMarkers);

        NumberAxis rangeAxis = new NumberAxis("Tempo di risposta R0 [s]");
        rangeAxis.setRange(0.0, Math.max(slaThreshold * 1.25, maxConvergedR0 * 1.15));
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        for (int i = 0; i < globalDataset.getSeriesCount(); i++) {
            String key = globalDataset.getSeriesKey(i).toString();
            boolean diverging = key.equals("Divergente");
            boolean inconclusive = key.equals("Saltata o inconclusiva");
            boolean skipped = diverging || inconclusive;
            renderer.setSeriesPaint(i, diverging ? NORD_RED : inconclusive ? NORD_ORANGE : PALETTE[i % PALETTE.length]);
            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            renderer.setSeriesLinesVisible(i, !skipped);
            renderer.setSeriesShapesVisible(i, true);
            if (skipped) {
                renderer.setSeriesShape(i, diverging ? ShapeUtils.createDiagonalCross(4.0f, 1.5f) : ShapeUtils.createUpTriangle(4.0f));
            }
        }
        mainPlot.setDataset(globalDataset); mainPlot.setRenderer(renderer); mainPlot.setRangeAxis(rangeAxis);
        setupPlot(mainPlot); applyLimit(mainPlot, slaThreshold, "SLA");
        JFreeChart chart = new JFreeChart("Frontiera scaling orizzontale", JFreeChart.DEFAULT_TITLE_FONT, mainPlot, true);
        chart.setBackgroundPaint(Color.WHITE);
        saveChart(chart, outputPath, 1100, 720);
    }

    /**
     * Generates a burstiness chart with response-time mean, dashed 95% confidence
     * bounds, and a separate standard-deviation subplot.
     *
     * @param meanDataset   Mean response-time series.
     * @param lowerDataset  Lower 95% CI response-time series.
     * @param upperDataset  Upper 95% CI response-time series.
     * @param stdDevDataset Response-time standard-deviation series.
     * @param outputPath    The path to save the generated image.
     * @param slaThreshold  The SLA threshold for response time.
     */
    public static void generateBurstinessMetricChart(
            XYSeriesCollection meanDataset,
            XYSeriesCollection lowerDataset,
            XYSeriesCollection upperDataset,
            XYSeriesCollection stdDevDataset,
            String title,
            String yAxisLabel,
            String outputPath,
            Double slaThreshold) {

        YIntervalSeriesCollection ciDataset = new YIntervalSeriesCollection();
        YIntervalSeriesCollection stdDevIntervalDataset = new YIntervalSeriesCollection();
        XYSeriesCollection ciBoundaryDataset = new XYSeriesCollection();

        for (int s = 0; s < meanDataset.getSeriesCount(); s++) {
            XYSeries mean = meanDataset.getSeries(s);
            XYSeries lower = lowerDataset.getSeries(s);
            XYSeries upper = upperDataset.getSeries(s);
            XYSeries stdDev = stdDevDataset.getSeries(s);

            YIntervalSeries ciSeries = new YIntervalSeries(mean.getKey());
            YIntervalSeries stdDevSeries = new YIntervalSeries(mean.getKey() + " deviazione standard");
            XYSeries lowerBoundary = new XYSeries(mean.getKey() + " limite inferiore CI 95%");
            XYSeries upperBoundary = new XYSeries(mean.getKey() + " limite superiore CI 95%");
            for (int i = 0; i < mean.getItemCount(); i++) {
                double x = mean.getX(i).doubleValue();
                double y = mean.getY(i).doubleValue();
                double lowerCi = lower.getY(i).doubleValue();
                double upperCi = upper.getY(i).doubleValue();
                double sigma = stdDev.getY(i).doubleValue();
                ciSeries.add(x, y, lowerCi, upperCi);
                stdDevSeries.add(x, y, Math.max(0.0, y - sigma), y + sigma);
                lowerBoundary.add(x, lowerCi);
                upperBoundary.add(x, upperCi);
            }
            ciDataset.addSeries(ciSeries);
            stdDevIntervalDataset.addSeries(stdDevSeries);
            ciBoundaryDataset.addSeries(lowerBoundary);
            ciBoundaryDataset.addSeries(upperBoundary);
        }

        NumberAxis domainAxis = new NumberAxis("Tasso di arrivo lambda [job/s]");
        NumberAxis responseAxis = new NumberAxis(yAxisLabel);
        responseAxis.setAutoRangeIncludesZero(true);
        configurePercentageAxis(responseAxis);

        DeviationRenderer ciRenderer = new DeviationRenderer(true, true);
        ciRenderer.setAlpha(0.16f);
        for (int i = 0; i < ciDataset.getSeriesCount(); i++) {
            Color color = PALETTE[i % PALETTE.length];
            ciRenderer.setSeriesPaint(i, color);
            ciRenderer.setSeriesFillPaint(i, new Color(color.getRed(), color.getGreen(), color.getBlue(), 55));
            ciRenderer.setSeriesStroke(i, new BasicStroke(2.0f));
            ciRenderer.setSeriesShapesVisible(i, true);
        }

        XYPlot plot = new XYPlot(ciDataset, domainAxis, responseAxis, ciRenderer);
        setupPlot(plot);

        XYLineAndShapeRenderer boundaryRenderer = new XYLineAndShapeRenderer(true, false);
        BasicStroke dashed = new BasicStroke(1.1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5, 5}, 0);
        for (int i = 0; i < ciBoundaryDataset.getSeriesCount(); i++) {
            Color color = PALETTE[(i / 2) % PALETTE.length];
            boundaryRenderer.setSeriesPaint(i, new Color(color.getRed(), color.getGreen(), color.getBlue(), 145));
            boundaryRenderer.setSeriesStroke(i, dashed);
            boundaryRenderer.setSeriesVisibleInLegend(i, false);
        }
        plot.setDataset(1, ciBoundaryDataset);
        plot.setRenderer(1, boundaryRenderer);

        YIntervalRenderer stdDevRenderer = new YIntervalRenderer();
        for (int i = 0; i < stdDevIntervalDataset.getSeriesCount(); i++) {
            stdDevRenderer.setSeriesPaint(i, PALETTE[i % PALETTE.length]);
            stdDevRenderer.setSeriesStroke(i, new BasicStroke(1.1f));
            stdDevRenderer.setSeriesVisibleInLegend(i, false);
        }
        plot.setDataset(2, stdDevIntervalDataset);
        plot.setRenderer(2, stdDevRenderer);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        if (slaThreshold != null) {
            applyLimit(plot, slaThreshold, "SLA");
        }

        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setBackgroundPaint(Color.WHITE);
        saveChart(chart, outputPath, 1100, 700);
    }

    /**
     * Generates a single transient_h.txt chart for the complete architecture.
     *
     * @param dataset      Replication time series.
     * @param title        The chart title.
     * @param outputPath   The path to save the generated image.
     * @param slaThreshold The SLA threshold for response time.
     */
    public static void generateTransientChart(
            XYSeriesCollection dataset, String title, String outputPath, double slaThreshold) {

        TimeScale timeScale = transientTimeScale(dataset);
        XYSeriesCollection scaledDataset = scaleDomain(dataset, timeScale.factor());

        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                "Tempo [" + timeScale.label() + "]",
                "Tempo di risposta R0 [s]",
                scaledDataset,
                PlotOrientation.VERTICAL,
                true,
                false,
                false
        );

        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = chart.getXYPlot();
        setupPlot(plot);
        // applyTransientDomainRange(plot, scaledDataset);
        // applyRobustTransientRange(plot, scaledDataset);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        for (int i = 0; i < scaledDataset.getSeriesCount(); i++) {
            Color color = PALETTE[i % PALETTE.length];
            renderer.setSeriesPaint(i, new Color(color.getRed(), color.getGreen(), color.getBlue(), 175));
            renderer.setSeriesStroke(i, THIN_STROKE);
        }
        plot.setRenderer(renderer);

        if (isLimitVisible(plot, slaThreshold)) {
            applyLimit(plot, slaThreshold, "SLA");
        }
        saveChart(chart, outputPath, 1100, 650);
    }

    private static void applyTransientDomainRange(XYPlot plot, XYSeriesCollection dataset) {
        double maxDomain = 0.0;
        for (int s = 0; s < dataset.getSeriesCount(); s++) {
            XYSeries series = dataset.getSeries(s);
            for (int i = 0; i < series.getItemCount(); i++) {
                maxDomain = Math.max(maxDomain, series.getX(i).doubleValue());
            }
        }

        if (maxDomain <= 25.0) {
            return;
        }

        double visibleUpper = Math.min(maxDomain, Math.max(25.0, maxDomain * 0.75));
        ((NumberAxis) plot.getDomainAxis()).setRange(0.0, visibleUpper);
    }

    private static TimeScale transientTimeScale(XYSeriesCollection dataset) {
        double maxSeconds = 0.0;
        for (int s = 0; s < dataset.getSeriesCount(); s++) {
            XYSeries series = dataset.getSeries(s);
            for (int i = 0; i < series.getItemCount(); i++) {
                maxSeconds = Math.max(maxSeconds, series.getX(i).doubleValue());
            }
        }

        if (maxSeconds >= 3_600.0) {
            return new TimeScale(3_600.0, "ore");
        }
        if (maxSeconds >= 60.0) {
            return new TimeScale(60.0, "minuti");
        }
        return new TimeScale(1.0, "s");
    }

    private static void applyRobustTransientRange(XYPlot plot, XYSeriesCollection dataset) {
        List<Double> values = new ArrayList<>();
        for (int s = 0; s < dataset.getSeriesCount(); s++) {
            XYSeries series = dataset.getSeries(s);
            for (int i = 0; i < series.getItemCount(); i++) {
                double value = series.getY(i).doubleValue();
                if (Double.isFinite(value)) {
                    values.add(value);
                }
            }
        }

        if (values.size() < 5) {
            plot.getRangeAxis().setAutoRange(true);
            return;
        }

        Collections.sort(values);
        double lower = percentile(values, 0.03);
        double upper = percentile(values, 0.97);
        double span = Math.max(upper - lower, 0.15);
        double margin = Math.max(span * 0.45, 0.15);
        ((NumberAxis) plot.getRangeAxis()).setRange(Math.max(0.0, lower - margin), upper + margin);
    }

    private static boolean isLimitVisible(XYPlot plot, double limit) {
        return limit >= plot.getRangeAxis().getLowerBound() && limit <= plot.getRangeAxis().getUpperBound();
    }

    private static double percentile(List<Double> sortedValues, double p) {
        double index = p * (sortedValues.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return sortedValues.get(lower);
        }
        double weight = index - lower;
        return sortedValues.get(lower) * (1.0 - weight) + sortedValues.get(upper) * weight;
    }

    private static XYSeriesCollection scaleDomain(XYSeriesCollection dataset, double factor) {
        if (factor == 1.0) {
            return dataset;
        }

        XYSeriesCollection scaledDataset = new XYSeriesCollection();
        for (int s = 0; s < dataset.getSeriesCount(); s++) {
            XYSeries source = dataset.getSeries(s);
            XYSeries scaled = new XYSeries(source.getKey(), false, true);
            for (int i = 0; i < source.getItemCount(); i++) {
                scaled.add(source.getX(i).doubleValue() / factor, source.getY(i));
            }
            scaledDataset.addSeries(scaled);
        }
        return scaledDataset;
    }

    private record TimeScale(double factor, String label) { }

    private static void setupPlot(XYPlot plot) {
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
    }

    private static void configurePercentageAxis(NumberAxis axis) {
        String label = axis.getLabel();
        if (label != null && label.contains("[%]")) {
            axis.setRange(0.0, 100.0);
            axis.setAutoRange(false);
        }
    }

    private static XYPlot createXYSubplot(XYSeriesCollection dataset, String label, XYLineAndShapeRenderer renderer) {
        NumberAxis axis = new NumberAxis(label);
        axis.setAutoRangeIncludesZero(false);
        configurePercentageAxis(axis);
        XYPlot plot = new XYPlot(dataset, null, axis, renderer);
        plot.setBackgroundPaint(Color.WHITE);
        return plot;
    }

    private static void configureLegend(JFreeChart chart, XYSeriesCollection series, XYLineAndShapeRenderer renderer) {
        LegendTitle legend = chart.getLegend();
        if (legend != null) {
            legend.setSources(new LegendItemSource[] {
                () -> {
                    LegendItemCollection result = new LegendItemCollection();
                    for (int i = 0; i < series.getSeriesCount(); i++) result.add(renderer.getLegendItem(0, i));
                    return result;
                }
            });
        }
    }

    private static void configureCvLegend(JFreeChart chart, XYSeriesCollection series) {
        LegendTitle legend = chart.getLegend();
        if (legend != null) {
            legend.setSources(new LegendItemSource[] {
                () -> {
                    LegendItemCollection result = new LegendItemCollection();
                    for (int i = 0; i < series.getSeriesCount(); i++) {
                        Color color = PALETTE[i % PALETTE.length];
                        result.add(new LegendItem(
                                series.getSeriesKey(i).toString(),
                                null,
                                null,
                                null,
                                new Line2D.Double(0, 0, 12, 0),
                                new BasicStroke(2.0f),
                                color
                        ));
                    }
                    return result;
                }
            });
        }
    }

    private static XYLineAndShapeRenderer createCustomRenderer() {
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        for (int i = 0; i < PALETTE.length; i++) { 
            renderer.setSeriesPaint(i, PALETTE[i]); 
            renderer.setSeriesStroke(i, new BasicStroke(2.0f)); 
        }
        return renderer;
    }

    private static class ClippedColoredStatisticalBarRenderer extends StatisticalBarRenderer {
        @Override public Paint getItemPaint(int r, int c) { return PALETTE[c % PALETTE.length]; }
    }
}

