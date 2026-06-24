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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
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

    private static final List<BurstinessMetric> BURSTINESS_METRICS = List.of(
            new BurstinessMetric("response_time", "Burstiness - Response Time", "Response Time R0 (s)", true),
            new BurstinessMetric("jobs_in_system", "Burstiness - Jobs in System", "Jobs in System", false),
            new BurstinessMetric("system_utilization", "Burstiness - System Utilization", "System Utilization", false),
            new BurstinessMetric("throughput", "Burstiness - Throughput", "Throughput", false),
            new BurstinessMetric("diverted_jobs", "Burstiness - Diverted Jobs", "Diverted Jobs", false),
            new BurstinessMetric("avg_servers", "Burstiness - Avg Active Servers", "Avg Active Servers (N)", false),
            new BurstinessMetric("scale_out_actions", "Burstiness - Scale OUT Actions", "Scale OUT Actions", false),
            new BurstinessMetric("scale_in_actions", "Burstiness - Scale IN Actions", "Scale IN Actions", false),
            new BurstinessMetric("scale_up_actions", "Burstiness - Scale UP Actions", "Scale UP Actions", false),
            new BurstinessMetric("scale_down_actions", "Burstiness - Scale DOWN Actions", "Scale DOWN Actions", false),
            new BurstinessMetric("spike_avg_speed", "Burstiness - Spike Server Avg Speed", "Spike Server Avg Speed", false),
            new BurstinessMetric("spike_utilization", "Burstiness - Spike Server Utilization", "Spike Server Utilization", false)
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
            logger.info("Usage: ObjectiveChartUtility <all|horizontal|simax|vertical-step|routing|burstiness|cost|vertical-stability> [inputCsv] [outputPng]");
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
            case "vertical-stability", "stability" -> regenerateVerticalStabilityChart(defaultBandCsvPath(), "data/objective/band/vertical_scaler_stability.png");
            default -> logger.info("Unknown chart '{}'. Available charts: all, horizontal, simax, vertical-step, routing, burstiness, cost, vertical-stability", args[0]);
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
    }

    private static void regenerateIfPresent(String csvPath, ChartRegenerator regenerator) throws IOException {
        if (Files.exists(Path.of(csvPath))) {
            regenerator.regenerate();
        } else {
            logger.info("Skipping missing CSV: {}", csvPath);
        }
    }

    private static void regenerateSiMaxChart(String inputCsv, String outputPng) throws IOException {
        XYSeries rtMean = new XYSeries("Mean R0");
        XYSeries rtLower = new XYSeries("Lower 95% CI");
        XYSeries rtUpper = new XYSeries("Upper 95% CI");
        XYSeries spikeCount = new XYSeries("Diverted Jobs (count)");
        XYSeries spikeUtil = new XYSeries("Spike Utilization (%)");

        for (String line : dataLines(inputCsv)) {
            String[] raw = line.split(",");
            int siMax = Integer.parseInt(raw[0].trim());
            rtMean.add(siMax, commaDecimal(raw, 1));
            rtLower.add(siMax, commaDecimal(raw, 3));
            rtUpper.add(siMax, commaDecimal(raw, 5));
            spikeCount.add(siMax, commaDecimal(raw, 7));
            spikeUtil.add(siMax, commaDecimal(raw, 11));
        }

        generateSiMaxEstimationStackedChart(rtMean, rtLower, rtUpper, spikeCount, spikeUtil, outputPng, ApplicationConfig.SLA_THRESHOLD);
    }

    private static void regenerateVerticalStepChart(String inputCsv, String outputPng) throws IOException {
        XYSeries rtSeries = new XYSeries("Response Time R0");
        XYSeries speedSeries = new XYSeries("Avg Speed Multiplier");
        XYSeries utilSeries = new XYSeries("Utilization (%)");

        for (String line : dataLines(inputCsv)) {
            String[] raw = line.split(",");
            double increment = commaDecimal(raw, 0);
            rtSeries.add(increment, commaDecimal(raw, 2));
            speedSeries.add(increment, commaDecimal(raw, 6));
            utilSeries.add(increment, raw.length > 10 ? commaDecimal(raw, 10) : 0.0);
        }

        generateVerticalSizingStackedChart(rtSeries, speedSeries, utilSeries, outputPng, ApplicationConfig.SLA_THRESHOLD);
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
            XYSeries lower = new XYSeries("CV=" + entry.getKey() + " Lower 95% CI");
            XYSeries upper = new XYSeries("CV=" + entry.getKey() + " Upper 95% CI");
            XYSeries stdDev = new XYSeries("CV=" + entry.getKey() + " StdDev");
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
                "Response Time vs Burstiness", "Response Time R0 (s)", outputPng, ApplicationConfig.SLA_THRESHOLD);
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

        for (String line : lines.stream().skip(1).filter(l -> !l.isBlank()).toList()) {
            String[] raw = line.split(",");
            double cv = Double.parseDouble(raw[0].trim());
            double lambda = Double.parseDouble(raw[1].trim());
            double mean = Double.parseDouble(raw[meanIndex].trim());
            double stdDev = Double.parseDouble(raw[stdDevIndex].trim());
            double lower = Double.parseDouble(raw[lowIndex].trim());
            double upper = Double.parseDouble(raw[highIndex].trim());
            meanByCv.computeIfAbsent(cv, ignored -> new XYSeries("CV=" + cv)).add(lambda, mean);
            lowerByCv.computeIfAbsent(cv, ignored -> new XYSeries("CV=" + cv + " Lower 95% CI")).add(lambda, lower);
            upperByCv.computeIfAbsent(cv, ignored -> new XYSeries("CV=" + cv + " Upper 95% CI")).add(lambda, upper);
            stdDevByCv.computeIfAbsent(cv, ignored -> new XYSeries("CV=" + cv + " StdDev")).add(lambda, stdDev);
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
            String label = "ScaleIn " + scaleIn;
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
                    new XYSeries(label + " Lower 95% CI"),
                    new XYSeries(label + " Upper 95% CI"),
                    new XYSeries(label + " StdDev")
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

        double cv = decimal(raw, 0);
        double lambda = decimal(raw, 2);
        int servers = Integer.parseInt(raw[4].trim());
        double r0 = parseOptionalDecimal(raw, 5);
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
        return Double.parseDouble(raw[integerIndex].trim() + "." + raw[integerIndex + 1].trim());
    }

    private static double parseOptionalDecimal(String[] raw, int integerIndex) {
        if ("NaN".equals(raw[integerIndex].trim())) {
            return Double.NaN;
        }
        return decimal(raw, integerIndex);
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

        NumberAxis rtAxis = new NumberAxis("Response Time R0 (s)");
        XYLineAndShapeRenderer rtRenderer = new XYLineAndShapeRenderer(true, true);
        rtRenderer.setSeriesPaint(0, Color.BLUE);
        rtRenderer.setSeriesStroke(0, new BasicStroke(2.0f));

        Color lightBlue = new Color(0, 0, 255, 100);
        BasicStroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5, 5}, 0);
        rtRenderer.setSeriesPaint(1, lightBlue);
        rtRenderer.setSeriesStroke(1, dashed);
        rtRenderer.setSeriesShapesVisible(1, false);
        rtRenderer.setSeriesPaint(2, lightBlue);
        rtRenderer.setSeriesStroke(2, dashed);
        rtRenderer.setSeriesShapesVisible(2, false);

        XYPlot rtPlot = new XYPlot(rtDataset, null, rtAxis, rtRenderer);
        setupPlot(rtPlot);
        applyLimit(rtPlot, slaThreshold, "SLA");

        XYSeriesCollection countDataset = new XYSeriesCollection(spikeCount);
        NumberAxis countAxis = new NumberAxis("Diverted Jobs (count)");
        XYLineAndShapeRenderer countRenderer = new XYLineAndShapeRenderer(true, true);
        countRenderer.setSeriesPaint(0, NORD_RED);

        XYPlot spikePlot = new XYPlot(countDataset, null, countAxis, countRenderer);
        setupPlot(spikePlot);

        NumberAxis utilAxis = new NumberAxis("Spike Utilization (%)");
        utilAxis.setRange(0, 105);
        spikePlot.setRangeAxis(1, utilAxis);
        spikePlot.setDataset(1, new XYSeriesCollection(spikeUtil));
        spikePlot.mapDatasetToRangeAxis(1, 1);

        XYLineAndShapeRenderer utilRenderer = new XYLineAndShapeRenderer(true, true);
        utilRenderer.setSeriesPaint(0, NORD_CYAN);
        spikePlot.setRenderer(1, utilRenderer);

        NumberAxis domainAxis = new NumberAxis("SI_max Threshold");
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(domainAxis);
        combinedPlot.add(rtPlot, 2);
        combinedPlot.add(spikePlot, 1);
        combinedPlot.setGap(15.0);

        JFreeChart chart = new JFreeChart("SI_max Calibration (1.1)", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, true);
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

        XYPlot rtPlot = createLineSubplot(rtSeries, "Response Time (s)", Color.BLUE);
        applyLimit(rtPlot, slaThreshold, "SLA");
        XYPlot speedPlot = createLineSubplot(speedSeries, "Avg Speed Mult", NORD_RED);
        XYPlot utilPlot = createLineSubplot(utilSeries, "Utilization (%)", NORD_TEAL);

        NumberAxis domainAxis = new NumberAxis("Incremental Step Size");
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(domainAxis);
        combinedPlot.add(rtPlot, 1);
        combinedPlot.add(speedPlot, 1);
        combinedPlot.add(utilPlot, 1);
        combinedPlot.setGap(10.0);

        JFreeChart chart = new JFreeChart("Vertical Sizing Analysis (1.2)", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, true);
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
                barDataset.add(r0Map.get(policy), stdDevMap.get(policy), "Mean R0", policy));

        CategoryPlot plot = new CategoryPlot();
        plot.setDomainAxis(new CategoryAxis("Routing Policy"));

        double maxR0 = r0Map.values().stream().max(Double::compareTo).orElse(1.0);
        double maxStdDev = stdDevMap.values().stream().max(Double::compareTo).orElse(0.0);
        NumberAxis rangeAxis = new NumberAxis("Time (s)");
        rangeAxis.setRange(0.0, (maxR0 + maxStdDev) * 1.20);
        plot.setRangeAxis(rangeAxis);

        ClippedColoredStatisticalBarRenderer barRenderer = new ClippedColoredStatisticalBarRenderer();
        barRenderer.setBarPainter(new StandardBarPainter());
        barRenderer.setShadowVisible(false);
        plot.setDataset(0, barDataset);
        plot.setRenderer(0, barRenderer);

        JFreeChart chart = new JFreeChart("Routing Analysis (2.1) - Batch Means", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
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
            barDataset.add(r0Map.get(policy), stdDevMap.get(policy), "Mean R0", policy);
            lineDataset.addValue(p99Map.get(policy), "99th Percentile", policy);
        });

        CategoryPlot plot = new CategoryPlot();
        plot.setDomainAxis(new CategoryAxis("Routing Policy"));
        double maxP99 = p99Map.values().stream().max(Double::compareTo).orElse(3.0);
        NumberAxis rangeAxis = new NumberAxis("Time (s)");
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

        JFreeChart chart = new JFreeChart("Routing Analysis (2.1)", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setBackgroundPaint(Color.WHITE);
        saveChart(chart, outputPath, 1000, 700);
    }

    /**
     * Generates a grid of charts for cost analysis (Objective 3.1).
     *
     * @param costData     Map of cooldowns to cost datasets.
     * @param rtData       Map of cooldowns to response time datasets.
     * @param outputPath   The path to save the generated image.
     * @param slaThreshold The SLA threshold for response time.
     */
    public static void generateCostCooldownGrid(
            Map<Double, XYSeriesCollection> costData,
            Map<Double, XYSeriesCollection> rtData,
            String outputPath, double slaThreshold) {

        List<Double> sortedCooldowns = new ArrayList<>(costData.keySet());
        Collections.sort(sortedCooldowns);
        XYLineAndShapeRenderer sharedRenderer = createCustomRenderer();

        BufferedImage img = prepareImage(1200, 1000, "Economic & Performance Analysis (3.1)");
        Graphics2D g2 = img.createGraphics();

        int cellW = 1200 / 2;
        int cellH = (1000 - 100) / 2;

        int idx = 0;
        for (Double cd : sortedCooldowns) {
            NumberAxis domainAxis = new NumberAxis("Lambda [CD=" + cd + "s]");
            CombinedDomainXYPlot subplot = new CombinedDomainXYPlot(domainAxis);
            subplot.setGap(10.0);

            XYPlot costPlot = createXYSubplot(costData.get(cd), "Cost", sharedRenderer);
            XYPlot rtPlot = createXYSubplot(rtData.get(cd), "R0 (s)", sharedRenderer);
            applyLimit(rtPlot, slaThreshold, "SLA");

            subplot.add(costPlot, 1);
            subplot.add(rtPlot, 1);

            boolean showLegend = (idx == 1);
            JFreeChart subchart = createSubchart(subplot, showLegend);

            if (showLegend) {
                configureLegend(subchart, costData.get(sortedCooldowns.get(0)), sharedRenderer);
            }

            drawSubchart(g2, subchart, idx, cellW, cellH, 60);
            idx++;
        }
        finalizeImage(img, g2, outputPath);
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
            NumberAxis domainAxis = new NumberAxis("Arrival Rate (lambda)");
            CombinedDomainXYPlot plot = new CombinedDomainXYPlot(domainAxis);
            plot.setGap(12.0);

            XYPlot costPlot = createXYSubplot(costData.get(cd), "Total Cost", sharedRenderer);
            XYPlot rtPlot = createXYSubplot(rtData.get(cd), "R0 (s)", sharedRenderer);
            applyLimit(rtPlot, slaThreshold, "SLA");

            plot.add(costPlot, 1);
            plot.add(rtPlot, 1);

            JFreeChart chart = new JFreeChart(
                    "Economic & Performance Analysis - Cooldown " + formatCooldown(cd),
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
     * Generates a grid of charts for vertical scaler stability (Objective 4.1).
     *
     * @param bandStateData Map of bands to state change datasets.
     * @param bandRtData    Map of bands to response time datasets.
     * @param outputPath    The path to save the generated image.
     * @param slaThreshold  The SLA threshold for response time.
     */
    public static void generateVerticalScalerStabilityGrid(
            Map<Integer, XYSeriesCollection> bandStateData,
            Map<Integer, XYSeriesCollection> bandRtData,
            String outputPath, double slaThreshold) {

        List<Integer> sortedBands = new ArrayList<>(bandStateData.keySet());
        Collections.sort(sortedBands);

        Shape[] shapes = {
            new Ellipse2D.Double(-3, -3, 6, 6), // CV 1.0 (Circle)
            new Rectangle2D.Double(-3, -3, 6, 6), // CV 4.0 (Square)
            ShapeUtils.createUpTriangle(3f) // CV 10.0 (Triangle)
        };

        BufferedImage img = prepareImage(1200, 1000, "Vertical Scaler Stability & Performance (4.1)");
        Graphics2D g2 = img.createGraphics();

        int cellW = 1200 / 2;
        int cellH = (1000 - 100) / 2;

        int idx = 0;
        for (Integer band : sortedBands) {
            NumberAxis domainAxis = new NumberAxis("Lambda [Band=" + band + "]");
            CombinedDomainXYPlot subplot = new CombinedDomainXYPlot(domainAxis);
            subplot.setGap(10.0);

            XYLineAndShapeRenderer stateRenderer = new XYLineAndShapeRenderer(true, true);
            configureRenderer(stateRenderer, bandStateData.get(band).getSeriesCount(), NORD_RED, shapes);

            XYPlot statePlot = createXYSubplot(bandStateData.get(band), "Spike Utilization (%)", stateRenderer);

            XYLineAndShapeRenderer rtRenderer = new XYLineAndShapeRenderer(true, true);
            configureRenderer(rtRenderer, bandRtData.get(band).getSeriesCount(), NORD_BLUE, shapes);

            XYPlot rtPlot = createXYSubplot(bandRtData.get(band), "R0 (s)", rtRenderer);
            applyLimit(rtPlot, slaThreshold, "SLA");

            subplot.add(statePlot, 1);
            subplot.add(rtPlot, 1);

            boolean showLegend = (idx == 1);
            JFreeChart subchart = createSubchart(subplot, showLegend);

            if (showLegend) {
                configureStabilityLegend(subchart, bandStateData.get(sortedBands.get(0)), shapes);
            }

            drawSubchart(g2, subchart, idx, cellW, cellH, 60);
            idx++;
        }
        finalizeImage(img, g2, outputPath);
    }

    /**
     * Generates one vertical scaler stability chart for each hysteresis band.
     *
     * @param bandStateData Map of bands to spike-utilization datasets.
     * @param bandRtData    Map of bands to response-time datasets.
     * @param outputPath    Base path used to derive one output image per band.
     * @param slaThreshold  The SLA threshold for response time.
     */
    public static void generateVerticalScalerStabilityCharts(
            Map<Integer, XYSeriesCollection> bandStateData,
            Map<Integer, XYSeriesCollection> bandRtData,
            String outputPath, double slaThreshold) {

        List<Integer> sortedBands = new ArrayList<>(bandStateData.keySet());
        Collections.sort(sortedBands);

        Shape[] shapes = {
            new Ellipse2D.Double(-3, -3, 6, 6),
            new Rectangle2D.Double(-3, -3, 6, 6),
            ShapeUtils.createUpTriangle(3f)
        };

        for (Integer band : sortedBands) {
            NumberAxis domainAxis = new NumberAxis("Arrival Rate (lambda)");
            CombinedDomainXYPlot plot = new CombinedDomainXYPlot(domainAxis);
            plot.setGap(12.0);

            XYLineAndShapeRenderer utilRenderer = new XYLineAndShapeRenderer(true, true);
            configureRenderer(utilRenderer, bandStateData.get(band).getSeriesCount(), NORD_RED, shapes);
            XYPlot utilPlot = createXYSubplot(bandStateData.get(band), "Spike Utilization (%)", utilRenderer);

            XYLineAndShapeRenderer rtRenderer = new XYLineAndShapeRenderer(true, true);
            configureRenderer(rtRenderer, bandRtData.get(band).getSeriesCount(), NORD_BLUE, shapes);
            XYPlot rtPlot = createXYSubplot(bandRtData.get(band), "R0 (s)", rtRenderer);
            applyLimit(rtPlot, slaThreshold, "SLA");

            plot.add(utilPlot, 1);
            plot.add(rtPlot, 1);

            JFreeChart chart = new JFreeChart(
                    "Vertical Scaler Stability - Band " + band,
                    JFreeChart.DEFAULT_TITLE_FONT,
                    plot,
                    true
            );
            chart.setBackgroundPaint(Color.WHITE);
            configureStabilityLegend(chart, bandStateData.get(band), shapes);
            saveChart(chart, bandOutputPath(outputPath, band), 1000, 650);
        }
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
            NumberAxis domainAxis = new NumberAxis("Arrival Rate (lambda)");
            CombinedDomainXYPlot plot = new CombinedDomainXYPlot(domainAxis);
            plot.setGap(12.0);

            if (systemData.containsKey(band)) {
                plot.add(createStabilityMetricSubplot(systemData.get(band), "System Utilization (%)", null), 1);
            }
            plot.add(createStabilityMetricSubplot(spikeData.get(band), "Spike Utilization (%)", null), 1);
            plot.add(createStabilityMetricSubplot(rtData.get(band), "R0 (s)", slaThreshold), 1);

            JFreeChart chart = new JFreeChart(
                    "Vertical Scaler Stability - Band " + band,
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
            YIntervalSeries stdDevSeries = new YIntervalSeries(mean.getKey() + " StdDev");
            XYSeries lowerBoundary = new XYSeries(mean.getKey() + " Lower 95% CI");
            XYSeries upperBoundary = new XYSeries(mean.getKey() + " Upper 95% CI");

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
        mainPlot.setDomainAxis(new NumberAxis("Arrival Rate (lambda)"));
        List<Double> sortedCvs = new ArrayList<>(cvResults.keySet());
        Collections.sort(sortedCvs);

        XYSeriesCollection globalDataset = new XYSeriesCollection();
        XYSeries divergingMarkers = new XYSeries("Diverging", false, true);
        XYSeries skippedMarkers = new XYSeries("Skipped / inconclusive", false, true);
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

        NumberAxis rangeAxis = new NumberAxis("Avg R0 (s)");
        rangeAxis.setRange(0.0, Math.max(slaThreshold * 1.25, maxConvergedR0 * 1.15));
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        for (int i = 0; i < globalDataset.getSeriesCount(); i++) {
            String key = globalDataset.getSeriesKey(i).toString();
            boolean diverging = key.equals("Diverging");
            boolean inconclusive = key.equals("Skipped / inconclusive");
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
        JFreeChart chart = new JFreeChart("Horizontal Scaling Limits (1.3)", JFreeChart.DEFAULT_TITLE_FONT, mainPlot, true);
        chart.setBackgroundPaint(Color.WHITE);
        saveChart(chart, outputPath, 1100, 720);
    }

    /**
     * Generates a line chart for burstiness robustness (Objective 4.3).
     *
     * @param dataset      The dataset containing burstiness levels.
     * @param outputPath   The path to save the generated image.
     * @param slaThreshold The SLA threshold for response time.
     */
    public static void generateBurstinessLineChart(XYSeriesCollection dataset, String outputPath, double slaThreshold) {
        JFreeChart chart = ChartFactory.createXYLineChart("Robustness to Burstiness (4.3)", "Arrival Rate", "R0 (s)", dataset, PlotOrientation.VERTICAL, true, true, false);
        XYPlot plot = chart.getXYPlot();
        setupPlot(plot);

        XYLineAndShapeRenderer renderer = createCustomRenderer();
        plot.setRenderer(renderer);

        applyLimit(plot, slaThreshold, "SLA");
        saveChart(chart, outputPath, 1000, 600);
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
            YIntervalSeries stdDevSeries = new YIntervalSeries(mean.getKey() + " StdDev");
            XYSeries lowerBoundary = new XYSeries(mean.getKey() + " Lower 95% CI");
            XYSeries upperBoundary = new XYSeries(mean.getKey() + " Upper 95% CI");
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

        NumberAxis domainAxis = new NumberAxis("Arrival Rate");
        NumberAxis responseAxis = new NumberAxis(yAxisLabel);
        responseAxis.setAutoRangeIncludesZero(true);

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
     * Generates a grid of charts for transient_h.txt analysis (Objective 4.2).
     *
     * @param archData     Map of architectures to replication datasets.
     * @param title        The chart title.
     * @param outputPath   The path to save the generated image.
     * @param slaThreshold The SLA threshold for response time.
     */
    public static void generateTransientGrid(
            Map<String, XYSeriesCollection> archData, String title, String outputPath, double slaThreshold) {

        List<String> architectures = new ArrayList<>(archData.keySet());
        BufferedImage img = prepareImage(1200, 1000, title);
        Graphics2D g2 = img.createGraphics();

        int cellW = 1200 / 2;
        int cellH = (1000 - 50) / 2;

        for (int archIdx = 0; archIdx < architectures.size(); archIdx++) {
            String archName = architectures.get(archIdx);
            XYSeriesCollection dataset = archData.get(archName);

            NumberAxis xAxis = new NumberAxis("Time (s)");
            NumberAxis yAxis = new NumberAxis("R0 (s)");
            yAxis.setAutoRangeIncludesZero(true);

            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
            for (int i = 0; i < dataset.getSeriesCount(); i++) {
                renderer.setSeriesPaint(i, PALETTE[i % PALETTE.length]);
                renderer.setSeriesStroke(i, new BasicStroke(1.5f));
            }

            XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
            setupPlot(plot);
            applyLimit(plot, slaThreshold, "SLA");

            JFreeChart subchart = new JFreeChart(archName, new Font("SansSerif", Font.BOLD, 16), plot, false);
            subchart.setBackgroundPaint(Color.WHITE);

            drawSubchart(g2, subchart, archIdx, cellW, cellH, 50);
        }

        drawTransientLegend(g2, archData.get(architectures.get(0)), 1200, 1000);
        finalizeImage(img, g2, outputPath);
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
                "Time (" + timeScale.label() + ")",
                "R0 (s)",
                scaledDataset,
                PlotOrientation.VERTICAL,
                true,
                false,
                false
        );

        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = chart.getXYPlot();
        setupPlot(plot);
        plot.getRangeAxis().setAutoRange(true);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        for (int i = 0; i < scaledDataset.getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, PALETTE[i % PALETTE.length]);
            renderer.setSeriesStroke(i, new BasicStroke(1.5f));
        }
        plot.setRenderer(renderer);

        applyLimit(plot, slaThreshold, "SLA");
        saveChart(chart, outputPath, 1100, 650);
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
            return new TimeScale(3_600.0, "hours");
        }
        if (maxSeconds >= 60.0) {
            return new TimeScale(60.0, "minutes");
        }
        return new TimeScale(1.0, "s");
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

    private static XYPlot createXYSubplot(XYSeriesCollection dataset, String label, XYLineAndShapeRenderer renderer) {
        NumberAxis axis = new NumberAxis(label);
        axis.setAutoRangeIncludesZero(false);
        XYPlot plot = new XYPlot(dataset, null, axis, renderer);
        plot.setBackgroundPaint(Color.WHITE);
        return plot;
    }

    private static JFreeChart createSubchart(CombinedDomainXYPlot plot, boolean showLegend) {
        JFreeChart subchart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, showLegend);
        subchart.setBackgroundPaint(Color.WHITE);
        return subchart;
    }

    private static void configureRenderer(XYLineAndShapeRenderer renderer, int count, Color color, Shape[] shapes) {
        for (int i = 0; i < count; i++) {
            renderer.setSeriesPaint(i, color);
            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            renderer.setSeriesShape(i, shapes[i % shapes.length]);
        }
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

    private static void configureStabilityLegend(JFreeChart chart, XYSeriesCollection series, Shape[] shapes) {
        LegendTitle legend = chart.getLegend();
        if (legend != null) {
            legend.setSources(new LegendItemSource[] {
                () -> {
                    LegendItemCollection result = new LegendItemCollection();
                    result.add(new LegendItem("Spike Utilization (%)", null, null, null, new Line2D.Double(0, 0, 10, 0), new BasicStroke(2.0f), NORD_RED));
                    result.add(new LegendItem("Avg Response Time R0", null, null, null, new Line2D.Double(0, 0, 10, 0), new BasicStroke(2.0f), NORD_BLUE));
                    for (int i = 0; i < series.getSeriesCount(); i++) {
                        result.add(new LegendItem(series.getSeriesKey(i).toString(), null, null, null, shapes[i % shapes.length], Color.DARK_GRAY));
                    }
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

    private static BufferedImage prepareImage(int width, int height, String title) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (width - titleWidth) / 2, 35);
        g2.dispose();
        return img;
    }

    private static void drawSubchart(Graphics2D g2, JFreeChart subchart, int idx, int cellW, int cellH, int topOffset) {
        int x = (idx % 2) * cellW;
        int y = topOffset + (idx / 2) * cellH;
        subchart.draw(g2, new Rectangle(x, y, cellW, cellH));
    }

    private static void drawTransientLegend(Graphics2D g2, XYSeriesCollection sampleDataset, int width, int height) {
        int legendY = height - 80;
        int legendX = 50;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i < sampleDataset.getSeriesCount(); i++) {
            g2.setColor(PALETTE[i % PALETTE.length]);
            g2.fillRect(legendX, legendY, 20, 10);
            g2.setColor(Color.BLACK);
            g2.drawString(sampleDataset.getSeriesKey(i).toString(), legendX + 25, legendY + 10);
            legendX += 200;
            if (legendX > width - 200) { legendX = 50; legendY += 20; }
        }
    }

    private static void finalizeImage(BufferedImage img, Graphics2D g2, String outputPath) {
        g2.dispose();
        try {
            ImageIO.write(img, "png", new File(outputPath));
            logger.info("Chart generated: {}", outputPath);
        } catch (IOException e) {
            logger.error("Failed to save chart: {}", outputPath, e);
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

