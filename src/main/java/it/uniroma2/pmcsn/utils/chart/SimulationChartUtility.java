package it.uniroma2.pmcsn.utils.chart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to generate professional charts from simulation data.
 */
public class SimulationChartUtility {
    private static final Logger logger = LoggerFactory.getLogger(SimulationChartUtility.class);

    /**
     * Generates a detailed comparison chart of Web Server vs Spike Server load.
     *
     * @param csvPath    Path to the input CSV file.
     * @param outputPath Path where the chart image (PNG) will be saved.
     */
    public static void generateLoadComparisonChart(String csvPath, String outputPath) {
        XYSeries wsSeries = new XYSeries("Web Server (Active Jobs)");
        XYSeries ssSeries = new XYSeries("Spike Server (Overflow)");
        XYSeries arrivalSeries = new XYSeries("Individual Arrivals");

        double siMaxVal = 0;
        int maxJobsSeen = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String line = reader.readLine(); // Skip header
            if (line == null) return;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 7) continue;

                double clock = Double.parseDouble(parts[0]);
                String event = parts[1];
                int wsJobs = Integer.parseInt(parts[2]);
                int ssJobs = Integer.parseInt(parts[3]);
                siMaxVal = Double.parseDouble(parts[6]); // Extract fixed threshold

                wsSeries.add(clock, wsJobs);
                ssSeries.add(clock, ssJobs);

                if ("ARRIVAL".equalsIgnoreCase(event)) {
                    arrivalSeries.add(clock, 1.0);
                }

                maxJobsSeen = Math.max(maxJobsSeen, Math.max(wsJobs + ssJobs, (int)siMaxVal));
            }
        } catch (IOException | NumberFormatException e) {
            logger.error("Error reading simulation state file: {}", csvPath, e);
            return;
        }

        XYSeriesCollection stateDataset = new XYSeriesCollection();
        stateDataset.addSeries(wsSeries);
        stateDataset.addSeries(ssSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "System Load Dynamics: Web Server vs Spike Server",
                "Time (seconds)",
                "Number of Jobs",
                stateDataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(230, 230, 230));
        plot.setRangeGridlinePaint(new Color(230, 230, 230));
        plot.setAxisOffset(new RectangleInsets(10, 10, 10, 10));

        // State Renderer (Step Area)
        XYStepAreaRenderer stepRenderer = new XYStepAreaRenderer(XYStepAreaRenderer.AREA);
        stepRenderer.setSeriesPaint(0, new Color(94, 129, 172, 120));  // Nord Frost Blue
        stepRenderer.setSeriesPaint(1, new Color(191, 97, 106, 120));  // Nord Aurora Red
        plot.setRenderer(0, stepRenderer);

        // Fixed Threshold Line (ValueMarker)
        ValueMarker thresholdMarker = new ValueMarker(siMaxVal);
        thresholdMarker.setPaint(new Color(220, 20, 60)); // Crimson Red
        thresholdMarker.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{15, 5}, 0));
        thresholdMarker.setLabel("LIMIT (SI Max = " + (int)siMaxVal + ")");
        thresholdMarker.setLabelFont(new Font("SansSerif", Font.BOLD, 13));
        thresholdMarker.setLabelPaint(new Color(150, 0, 0));
        thresholdMarker.setLabelAnchor(org.jfree.chart.ui.RectangleAnchor.TOP_RIGHT);
        thresholdMarker.setLabelTextAnchor(org.jfree.chart.ui.TextAnchor.BOTTOM_RIGHT);
        plot.addRangeMarker(thresholdMarker);

        // Arrivals Renderer (Lime Green Circles) - LAYER 2 (TOP)
        XYSeriesCollection arrivalDataset = new XYSeriesCollection();
        arrivalDataset.addSeries(arrivalSeries);
        plot.setDataset(1, arrivalDataset);
        XYLineAndShapeRenderer arrivalRenderer = new XYLineAndShapeRenderer(false, true);
        arrivalRenderer.setSeriesPaint(0, new Color(0, 255, 0)); 
        arrivalRenderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8)); // Larger (8x8)
        plot.setRenderer(1, arrivalRenderer);

        try {
            ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1200, 800);
            logger.info("Enhanced focused load comparison chart generated at: {}", outputPath);
        } catch (IOException e) {
            logger.error("Failed to save chart image: {}", outputPath, e);
        }
    }

    public static void generateHorizontalScalingChart(String csvPath, String outputPath) {
        XYSeries activeServersSeries = new XYSeries("Active Web Servers");
        XYSeries hMetricSeries = new XYSeries("Avg Response Time (Window)");
        XYSeries cooldownSeries = new XYSeries("Residual Cooldown");
        XYSeries arrivalSeries = new XYSeries("Arrivals");
        XYSeries completionSeries = new XYSeries("Completions");

        double scaleUpLimit = 0;
        double scaleDownLimit = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String line = reader.readLine(); // Skip header
            if (line == null) return;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 11) continue;

                double clock = Double.parseDouble(parts[0]);
                String event = parts[1];
                int activeWebServers = Integer.parseInt(parts[2]);
                double hMetric = Double.parseDouble(parts[3]);
                scaleUpLimit = Double.parseDouble(parts[4]);
                scaleDownLimit = Double.parseDouble(parts[5]);
                double hCooldown = Double.parseDouble(parts[6]);

                if (clock > 500.0) continue; 

                activeServersSeries.add(clock, activeWebServers);
                hMetricSeries.add(clock, hMetric);
                cooldownSeries.add(clock, hCooldown);

                if ("ARRIVAL".equalsIgnoreCase(event)) {
                    arrivalSeries.add(clock, hMetric);
                } else if ("COMPLETION".equalsIgnoreCase(event)) {
                    completionSeries.add(clock, hMetric);
                }
            }
        } catch (IOException | NumberFormatException e) {
            logger.error("Error reading scaling metrics file: {}", csvPath, e);
            return;
        }

        XYSeriesCollection dataset1 = new XYSeriesCollection(activeServersSeries);
        XYSeriesCollection dataset2 = new XYSeriesCollection(hMetricSeries);
        XYSeriesCollection dataset3 = new XYSeriesCollection(cooldownSeries);
        
        XYSeriesCollection eventsDataset = new XYSeriesCollection();
        eventsDataset.addSeries(arrivalSeries);
        eventsDataset.addSeries(completionSeries);

        // Subplot 1: Active Servers
        NumberAxis rangeAxis1 = new NumberAxis("Active Servers");
        rangeAxis1.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        XYStepAreaRenderer stepRenderer = new XYStepAreaRenderer(XYStepAreaRenderer.AREA);
        stepRenderer.setSeriesPaint(0, new Color(94, 129, 172, 120)); // Nord Frost Blue
        XYPlot subplot1 = new XYPlot(dataset1, null, rangeAxis1, stepRenderer);
        subplot1.setBackgroundPaint(Color.WHITE);
        subplot1.setRangeGridlinePaint(new Color(230, 230, 230));

        // Subplot 2: Metric (Avg Response Time)
        NumberAxis rangeAxis2 = new NumberAxis("Avg Response Time");
        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
        lineRenderer.setSeriesPaint(0, new Color(191, 97, 106)); // Nord Aurora Red
        lineRenderer.setSeriesStroke(0, new BasicStroke(2.0f));
        XYPlot subplot2 = new XYPlot(dataset2, null, rangeAxis2, lineRenderer);
        subplot2.setBackgroundPaint(Color.WHITE);
        subplot2.setRangeGridlinePaint(new Color(230, 230, 230));

        // Add Events (X and O) on Subplot 2
        subplot2.setDataset(1, eventsDataset);
        XYLineAndShapeRenderer eventsRenderer = new XYLineAndShapeRenderer(false, true);
        // Arrival: Red X
        eventsRenderer.setSeriesPaint(0, Color.RED);
        eventsRenderer.setSeriesShape(0, org.jfree.chart.util.ShapeUtils.createDiagonalCross(3, 1));
        // Completion: Green O
        eventsRenderer.setSeriesPaint(1, new Color(34, 139, 34)); // Forest Green
        eventsRenderer.setSeriesShape(1, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));
        subplot2.setRenderer(1, eventsRenderer);

        // Subplot 3: Residual Cooldown
        NumberAxis rangeAxis3 = new NumberAxis("Cooldown (s)");
        XYLineAndShapeRenderer cooldownRenderer = new XYLineAndShapeRenderer(true, false);
        cooldownRenderer.setSeriesPaint(0, new Color(136, 192, 208)); // Nord Frost Light Blue
        XYPlot subplot3 = new XYPlot(dataset3, null, rangeAxis3, cooldownRenderer);
        subplot3.setBackgroundPaint(Color.WHITE);
        subplot3.setRangeGridlinePaint(new Color(230, 230, 230));

        // Threshold Markers on Subplot 2
        if (scaleUpLimit > 0) {
            ValueMarker upMarker = new ValueMarker(scaleUpLimit);
            upMarker.setPaint(new Color(220, 20, 60));
            upMarker.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10, 5}, 0));
            upMarker.setLabel("Scale UP Limit (" + String.format("%.2f", scaleUpLimit) + ")");
            upMarker.setLabelAnchor(org.jfree.chart.ui.RectangleAnchor.TOP_RIGHT);
            upMarker.setLabelTextAnchor(org.jfree.chart.ui.TextAnchor.BOTTOM_RIGHT);
            subplot2.addRangeMarker(upMarker, org.jfree.chart.ui.Layer.FOREGROUND);
        }

        if (scaleDownLimit > 0) {
            ValueMarker downMarker = new ValueMarker(scaleDownLimit);
            downMarker.setPaint(new Color(34, 139, 34)); // Forest Green
            downMarker.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10, 5}, 0));
            downMarker.setLabel("Scale DOWN Limit (" + String.format("%.2f", scaleDownLimit) + ")");
            downMarker.setLabelAnchor(org.jfree.chart.ui.RectangleAnchor.BOTTOM_RIGHT);
            downMarker.setLabelTextAnchor(org.jfree.chart.ui.TextAnchor.TOP_RIGHT);
            subplot2.addRangeMarker(downMarker, org.jfree.chart.ui.Layer.FOREGROUND);
        }

        NumberAxis domainAxis = new NumberAxis("Time (seconds)");
        CombinedDomainXYPlot plot = new CombinedDomainXYPlot(domainAxis);
        plot.setGap(10.0);
        plot.add(subplot1, 2); 
        plot.add(subplot2, 2);
        plot.add(subplot3, 1); 
        plot.setOrientation(PlotOrientation.VERTICAL);

        JFreeChart chart = new JFreeChart("Horizontal Scaling Dynamics", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setBackgroundPaint(Color.WHITE);

        try {
            ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1200, 1000);
            logger.info("Horizontal scaling chart generated at: {}", outputPath);
        } catch (IOException e) {
            logger.error("Failed to save chart image: {}", outputPath, e);
        }
    }

    /**
     * Generates a chart for Vertical Scaling dynamics.
     *
     * @param csvPath    Path to the input CSV file containing ScalingMetrics.
     * @param outputPath Path where the chart image (PNG) will be saved.
     */
    public static void generateVerticalScalingChart(String csvPath, String outputPath) {
        XYSeries spikeSpeedSeries = new XYSeries("Spike Server Speed Multiplier");
        XYSeries vMetricSeries = new XYSeries("Spike Server Metric (e.g. Load/Utilization)");
        XYSeries cooldownSeries = new XYSeries("Residual Cooldown");
        XYSeries arrivalSeries = new XYSeries("Arrivals");
        XYSeries completionSeries = new XYSeries("Completions");

        double vUpperLimit = 0;
        double vLowerLimit = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String line = reader.readLine(); // Skip header
            if (line == null) return;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 11) continue;

                double clock = Double.parseDouble(parts[0]);
                String event = parts[1];
                double spikeSpeed = Double.parseDouble(parts[7]);
                double vMetric = Double.parseDouble(parts[8]);
                vUpperLimit = Double.parseDouble(parts[9]);
                vLowerLimit = Double.parseDouble(parts[10]);
                double vCooldown = Double.parseDouble(parts[11]);

                if (clock > 500.0) continue;

                spikeSpeedSeries.add(clock, spikeSpeed);
                vMetricSeries.add(clock, vMetric);
                cooldownSeries.add(clock, vCooldown);

                if ("ARRIVAL".equalsIgnoreCase(event)) {
                    arrivalSeries.add(clock, vMetric);
                } else if ("COMPLETION".equalsIgnoreCase(event)) {
                    completionSeries.add(clock, vMetric);
                }
            }
        } catch (IOException | NumberFormatException e) {
            logger.error("Error reading scaling metrics file: {}", csvPath, e);
            return;
        }

        XYSeriesCollection dataset1 = new XYSeriesCollection(spikeSpeedSeries);
        XYSeriesCollection dataset2 = new XYSeriesCollection(vMetricSeries);
        XYSeriesCollection dataset3 = new XYSeriesCollection(cooldownSeries);
        
        XYSeriesCollection eventsDataset = new XYSeriesCollection();
        eventsDataset.addSeries(arrivalSeries);
        eventsDataset.addSeries(completionSeries);

        // Subplot 1: Speed Multiplier
        NumberAxis rangeAxis1 = new NumberAxis("Speed Multiplier");
        XYStepAreaRenderer stepRenderer = new XYStepAreaRenderer(XYStepAreaRenderer.AREA);
        stepRenderer.setSeriesPaint(0, new Color(163, 190, 140, 120)); // Nord Aurora Green
        XYPlot subplot1 = new XYPlot(dataset1, null, rangeAxis1, stepRenderer);
        subplot1.setBackgroundPaint(Color.WHITE);
        subplot1.setRangeGridlinePaint(new Color(230, 230, 230));

        // Subplot 2: Vertical Metric
        NumberAxis rangeAxis2 = new NumberAxis("Scaling Metric");
        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
        lineRenderer.setSeriesPaint(0, new Color(208, 135, 112)); // Nord Aurora Orange
        lineRenderer.setSeriesStroke(0, new BasicStroke(2.0f));
        XYPlot subplot2 = new XYPlot(dataset2, null, rangeAxis2, lineRenderer);
        subplot2.setBackgroundPaint(Color.WHITE);
        subplot2.setRangeGridlinePaint(new Color(230, 230, 230));

        // Add Events (X and O) on Subplot 2
        subplot2.setDataset(1, eventsDataset);
        XYLineAndShapeRenderer eventsRenderer = new XYLineAndShapeRenderer(false, true);
        // Arrival: Red X
        eventsRenderer.setSeriesPaint(0, Color.RED);
        eventsRenderer.setSeriesShape(0, org.jfree.chart.util.ShapeUtils.createDiagonalCross(3, 1));
        // Completion: Green O
        eventsRenderer.setSeriesPaint(1, new Color(34, 139, 34)); // Forest Green
        eventsRenderer.setSeriesShape(1, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));
        subplot2.setRenderer(1, eventsRenderer);

        // Subplot 3: Residual Cooldown
        NumberAxis rangeAxis3 = new NumberAxis("Cooldown (s)");
        XYLineAndShapeRenderer cooldownRenderer = new XYLineAndShapeRenderer(true, false);
        cooldownRenderer.setSeriesPaint(0, new Color(180, 142, 173)); // Nord Aurora Purple
        XYPlot subplot3 = new XYPlot(dataset3, null, rangeAxis3, cooldownRenderer);
        subplot3.setBackgroundPaint(Color.WHITE);
        subplot3.setRangeGridlinePaint(new Color(230, 230, 230));

        // Threshold Markers on Subplot 2
        if (vUpperLimit > 0) {
            ValueMarker upMarker = new ValueMarker(vUpperLimit);
            upMarker.setPaint(new Color(220, 20, 60));
            upMarker.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10, 5}, 0));
            upMarker.setLabel("Scale UP Limit (" + String.format("%.2f", vUpperLimit) + ")");
            upMarker.setLabelAnchor(org.jfree.chart.ui.RectangleAnchor.TOP_RIGHT);
            upMarker.setLabelTextAnchor(org.jfree.chart.ui.TextAnchor.BOTTOM_RIGHT);
            subplot2.addRangeMarker(upMarker, org.jfree.chart.ui.Layer.FOREGROUND);
        }

        if (vLowerLimit > 0) {
            ValueMarker downMarker = new ValueMarker(vLowerLimit);
            downMarker.setPaint(new Color(34, 139, 34)); // Forest Green
            downMarker.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10, 5}, 0));
            downMarker.setLabel("Scale DOWN Limit (" + String.format("%.2f", vLowerLimit) + ")");
            downMarker.setLabelAnchor(org.jfree.chart.ui.RectangleAnchor.BOTTOM_RIGHT);
            downMarker.setLabelTextAnchor(org.jfree.chart.ui.TextAnchor.TOP_RIGHT);
            subplot2.addRangeMarker(downMarker, org.jfree.chart.ui.Layer.FOREGROUND);
        }

        NumberAxis domainAxis = new NumberAxis("Time (seconds)");
        CombinedDomainXYPlot plot = new CombinedDomainXYPlot(domainAxis);
        plot.setGap(10.0);
        plot.add(subplot1, 2);
        plot.add(subplot2, 2);
        plot.add(subplot3, 1);
        plot.setOrientation(PlotOrientation.VERTICAL);

        JFreeChart chart = new JFreeChart("Vertical Scaling Dynamics", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setBackgroundPaint(Color.WHITE);

        try {
            ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1200, 1000);
            logger.info("Vertical scaling chart generated at: {}", outputPath);
        } catch (IOException e) {
            logger.error("Failed to save chart image: {}", outputPath, e);
        }
    }

    /**
     * Generates a chart to validate Routing Balance among Web Servers.
     * Uses a CombinedDomainXYPlot to show each server in its own vertical subplot.
     *
     * @param csvPath    Path to the input CSV file containing RoutingBalance metrics.
     * @param outputPath Path where the chart image (PNG) will be saved.
     */
    public static void generateRoutingBalanceChart(String csvPath, String outputPath) {
        Map<Integer, XYSeries> serverSeriesMap = new HashMap<>();
        List<String> serverNames = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String header = reader.readLine();
            if (header == null) return;
            String[] headers = header.split(",");
            
            // Find server columns
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].startsWith("server_")) {
                    XYSeries series = new XYSeries(headers[i]);
                    serverSeriesMap.put(i, series);
                    serverNames.add(headers[i]);
                }
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                double clock = Double.parseDouble(parts[0]);
                
                for (Map.Entry<Integer, XYSeries> entry : serverSeriesMap.entrySet()) {
                    double jobs = Double.parseDouble(parts[entry.getKey()]);
                    entry.getValue().add(clock, jobs);
                }
            }
        } catch (IOException | NumberFormatException e) {
            logger.error("Error reading routing balance file: {}", csvPath, e);
            return;
        }

        NumberAxis domainAxis = new NumberAxis("Time (seconds)");
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(domainAxis);
        combinedPlot.setGap(10.0);
        combinedPlot.setOrientation(PlotOrientation.VERTICAL);

        // Predefined colors for servers (Nord Theme inspired)
        Color[] colors = {
            new Color(94, 129, 172),  // Blue
            new Color(191, 97, 106),  // Red
            new Color(163, 190, 140), // Green
            new Color(208, 135, 112), // Orange
            new Color(180, 142, 173), // Purple
            new Color(235, 203, 139)  // Yellow
        };

        // Create a subplot for each server
        int colorIdx = 0;
        for (XYSeries series : serverSeriesMap.values()) {
            XYSeriesCollection dataset = new XYSeriesCollection(series);
            NumberAxis rangeAxis = new NumberAxis("Jobs (" + series.getKey() + ")");
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            
            // Use XYStepAreaRenderer for a solid "histogram-style" area
            XYStepAreaRenderer renderer = new XYStepAreaRenderer(XYStepAreaRenderer.AREA);
            renderer.setSeriesPaint(0, colors[colorIdx % colors.length]);
            
            XYPlot subplot = new XYPlot(dataset, null, rangeAxis, renderer);
            subplot.setBackgroundPaint(Color.WHITE);
            subplot.setRangeGridlinePaint(new Color(230, 230, 230));
            
            combinedPlot.add(subplot, 1);
            colorIdx++;
        }

        JFreeChart chart = new JFreeChart(
                "Routing Balance Validation (Individual Server Load)",
                JFreeChart.DEFAULT_TITLE_FONT,
                combinedPlot,
                true
        );
        chart.setBackgroundPaint(Color.WHITE);

        try {
            // Increase height based on number of servers (300px per server + margin)
            int height = Math.max(800, serverSeriesMap.size() * 250);
            ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1200, height);
            logger.info("Decomposed routing balance chart generated at: {}", outputPath);
        } catch (IOException e) {
            logger.error("Failed to save chart image: {}", outputPath, e);
        }
    }
}
