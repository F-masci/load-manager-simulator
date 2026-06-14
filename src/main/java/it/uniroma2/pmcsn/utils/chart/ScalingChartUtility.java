package it.uniroma2.pmcsn.utils.chart;

import it.uniroma2.pmcsn.utils.LogFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Utility for generating scaling dynamics charts (Horizontal and Vertical).
 */
public class ScalingChartUtility {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(ScalingChartUtility.class, "CHART");

    public static void generateHorizontalScalingChart(String csvPath, String outputPath) {
        XYSeries activeServersSeries = new XYSeries("Active Web Servers");
        XYSeries hMetricSeries = new XYSeries("Avg Response Time (Window)");
        XYSeries cooldownSeries = new XYSeries("Residual Cooldown");
        XYSeries arrivalSeries = new XYSeries("Arrivals");
        XYSeries completionSeries = new XYSeries("Completions");
        XYSeries checkSeries = new XYSeries("Scaling Checks");
        
        double scaleUpLimit = 0;
        double scaleDownLimit = 0;
        java.util.List<Double> scalingActionTimes = new java.util.ArrayList<>();
        int lastActiveServers = -1;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String line = reader.readLine(); 
            if (line == null) return;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 8) continue;

                double clock = Double.parseDouble(parts[0]);
                String event = parts[1];
                String check = parts[2];
                int activeWebServers = Integer.parseInt(parts[3]);
                double hMetric = Double.parseDouble(parts[4]);
                scaleUpLimit = Double.parseDouble(parts[5]);
                scaleDownLimit = Double.parseDouble(parts[6]);
                double hCooldown = Double.parseDouble(parts[7]);

                activeServersSeries.add(clock, activeWebServers);
                hMetricSeries.add(clock, hMetric);
                cooldownSeries.add(clock, hCooldown);

                if ("ARRIVAL".equalsIgnoreCase(event)) {
                    arrivalSeries.add(clock, hMetric);
                } else if ("COMPLETION".equalsIgnoreCase(event)) {
                    completionSeries.add(clock, hMetric);
                }
                
                if ("HORIZONTAL".equalsIgnoreCase(check) || "BOTH".equalsIgnoreCase(check)) {
                    checkSeries.add(clock, hMetric);
                }

                if (lastActiveServers != -1 && activeWebServers != lastActiveServers) {
                    scalingActionTimes.add(clock);
                }
                lastActiveServers = activeWebServers;
            }
        } catch (IOException | NumberFormatException e) {
            logger.error("Error reading horizontal scaling file: {}", csvPath, e);
            return;
        }

        XYPlot subplot1 = createAreaSubplot(activeServersSeries, "Active Servers", new Color(94, 129, 172, 120));
        XYPlot subplot2 = createLineSubplot(hMetricSeries, "Avg RT", new Color(191, 97, 106));
        XYPlot subplot3 = createLineSubplot(cooldownSeries, "Cooldown", new Color(136, 192, 208));

        applyThresholds(subplot2, scaleUpLimit, scaleDownLimit, "Scale UP", "Scale DOWN");
        applyEvents(subplot2, arrivalSeries, completionSeries, checkSeries);

        for (double t : scalingActionTimes) {
            ValueMarker marker = new ValueMarker(t);
            marker.setPaint(new Color(76, 86, 106, 180));
            marker.setStroke(new BasicStroke(1.5f));
            subplot1.addDomainMarker(marker);
            subplot2.addDomainMarker(marker);
        }

        CombinedDomainXYPlot plot = new CombinedDomainXYPlot(new NumberAxis("Time (seconds)"));
        plot.setGap(10.0);
        plot.add(subplot1, 2);
        plot.add(subplot2, 2);
        plot.add(subplot3, 1);
        plot.setOrientation(PlotOrientation.VERTICAL);

        JFreeChart chart = new JFreeChart("Horizontal Scaling Dynamics", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setBackgroundPaint(Color.WHITE);

        saveChart(chart, outputPath, 1200, 1000);
    }

    public static void generateVerticalScalingChart(String csvPath, String outputPath) {
        XYSeries spikeSpeedSeries = new XYSeries("Spike Server Speed Multiplier");
        XYSeries vMetricSeries = new XYSeries("Utilization (Mean Jobs)");
        XYSeries cooldownSeries = new XYSeries("Residual Cooldown");
        XYSeries arrivalSeries = new XYSeries("Arrivals");
        XYSeries completionSeries = new XYSeries("Completions");
        XYSeries checkSeries = new XYSeries("Scaling Checks");
        
        double vUpperLimit = 0;
        double vLowerLimit = 0;
        java.util.List<Double> speedActionTimes = new java.util.ArrayList<>();
        double lastSpeed = -1;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String line = reader.readLine();
            if (line == null) return;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 13) continue;

                double clock = Double.parseDouble(parts[0]);
                String event = parts[1];
                String check = parts[2];
                double spikeSpeed = Double.parseDouble(parts[8]);
                double vMetric = Double.parseDouble(parts[9]);
                vUpperLimit = Double.parseDouble(parts[10]);
                vLowerLimit = Double.parseDouble(parts[11]);
                double vCooldown = Double.parseDouble(parts[12]);

                spikeSpeedSeries.add(clock, spikeSpeed);
                vMetricSeries.add(clock, vMetric);
                cooldownSeries.add(clock, vCooldown);

                if ("ARRIVAL".equalsIgnoreCase(event)) {
                    arrivalSeries.add(clock, vMetric);
                } else if ("COMPLETION".equalsIgnoreCase(event)) {
                    completionSeries.add(clock, vMetric);
                }
                
                if ("VERTICAL".equalsIgnoreCase(check) || "BOTH".equalsIgnoreCase(check)) {
                    checkSeries.add(clock, vMetric);
                }

                if (lastSpeed != -1 && spikeSpeed != lastSpeed) {
                    speedActionTimes.add(clock);
                }
                lastSpeed = spikeSpeed;
            }
        } catch (IOException | NumberFormatException e) {
            logger.error("Error reading vertical scaling file: {}", csvPath, e);
            return;
        }

        XYPlot subplot1 = createAreaSubplot(spikeSpeedSeries, "Speed Multiplier", new Color(163, 190, 140, 120));
        XYPlot subplot2 = createLineSubplot(vMetricSeries, "Metric", new Color(208, 135, 112));
        XYPlot subplot3 = createLineSubplot(cooldownSeries, "Cooldown", new Color(180, 142, 173));

        applyThresholds(subplot2, vUpperLimit, vLowerLimit, "Speed UP", "Speed DOWN");
        applyEvents(subplot2, arrivalSeries, completionSeries, checkSeries);

        for (double t : speedActionTimes) {
            ValueMarker marker = new ValueMarker(t);
            marker.setPaint(new Color(76, 86, 106, 180));
            marker.setStroke(new BasicStroke(1.5f));
            subplot1.addDomainMarker(marker);
            subplot2.addDomainMarker(marker);
        }

        CombinedDomainXYPlot plot = new CombinedDomainXYPlot(new NumberAxis("Time (seconds)"));
        plot.setGap(10.0);
        plot.add(subplot1, 2);
        plot.add(subplot2, 2);
        plot.add(subplot3, 1);
        plot.setOrientation(PlotOrientation.VERTICAL);

        JFreeChart chart = new JFreeChart("Vertical Scaling Dynamics", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setBackgroundPaint(Color.WHITE);

        saveChart(chart, outputPath, 1200, 1000);
    }

    private static void applyThresholds(XYPlot plot, double up, double down, String upLabel, String downLabel) {
        if (up > 0) {
            ValueMarker upMarker = new ValueMarker(up);
            upMarker.setPaint(new Color(220, 20, 60));
            upMarker.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10, 5}, 0));
            upMarker.setLabel(upLabel + " Limit (" + String.format("%.2f", up) + ")");
            upMarker.setLabelAnchor(org.jfree.chart.ui.RectangleAnchor.TOP_RIGHT);
            upMarker.setLabelTextAnchor(org.jfree.chart.ui.TextAnchor.BOTTOM_RIGHT);
            plot.addRangeMarker(upMarker);
        }
        if (down > 0) {
            ValueMarker downMarker = new ValueMarker(down);
            downMarker.setPaint(new Color(34, 139, 34));
            downMarker.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10, 5}, 0));
            downMarker.setLabel(downLabel + " Limit (" + String.format("%.2f", down) + ")");
            downMarker.setLabelAnchor(org.jfree.chart.ui.RectangleAnchor.BOTTOM_RIGHT);
            downMarker.setLabelTextAnchor(org.jfree.chart.ui.TextAnchor.TOP_RIGHT);
            plot.addRangeMarker(downMarker);
        }
    }

    private static void applyEvents(XYPlot plot, XYSeries arrivals, XYSeries completions, XYSeries checks) {
        XYSeriesCollection eventsDataset = new XYSeriesCollection();
        eventsDataset.addSeries(arrivals);
        eventsDataset.addSeries(completions);
        eventsDataset.addSeries(checks);

        plot.setDataset(1, eventsDataset);
        XYLineAndShapeRenderer eventsRenderer = new XYLineAndShapeRenderer(false, true);

        // Arrivals: Red X (Alpha 0.7)
        eventsRenderer.setSeriesPaint(0, new Color(191, 97, 106, 180)); 
        eventsRenderer.setSeriesShape(0, org.jfree.chart.util.ShapeUtils.createDiagonalCross(4.5f, 1.5f));

        // Completions: Solid Green Circle (Alpha 0.7)
        eventsRenderer.setSeriesPaint(1, new Color(34, 139, 34, 180)); 
        eventsRenderer.setSeriesShape(1, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));

        // Scaling Checks: Blue Squares (Alpha 0.7)
        eventsRenderer.setSeriesPaint(2, new Color(0, 0, 255, 180)); 
        eventsRenderer.setSeriesShape(2, new java.awt.Rectangle(-3, -3, 6, 6));

        plot.setRenderer(1, eventsRenderer);
        plot.setDatasetRenderingOrder(org.jfree.chart.plot.DatasetRenderingOrder.FORWARD);
    }

    private static XYPlot createAreaSubplot(XYSeries series, String label, Color color) {
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        NumberAxis rangeAxis = new NumberAxis(label);
        XYStepAreaRenderer renderer = new XYStepAreaRenderer(XYStepAreaRenderer.AREA);
        renderer.setSeriesPaint(0, color);
        XYPlot subplot = new XYPlot(dataset, null, rangeAxis, renderer);
        subplot.setBackgroundPaint(Color.WHITE);
        subplot.setRangeGridlinePaint(new Color(230, 230, 230));
        return subplot;
    }

    private static XYPlot createLineSubplot(XYSeries series, String label, Color color) {
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        NumberAxis rangeAxis = new NumberAxis(label);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        XYPlot subplot = new XYPlot(dataset, null, rangeAxis, renderer);
        subplot.setBackgroundPaint(Color.WHITE);
        subplot.setRangeGridlinePaint(new Color(230, 230, 230));
        return subplot;
    }

    private static void saveChart(JFreeChart chart, String path, int width, int height) {
        try {
            ChartUtils.saveChartAsPNG(new File(path), chart, width, height);
            logger.info("Scaling chart generated: {}", path);
        } catch (IOException e) {
            logger.error("Failed to save scaling chart: {}", path, e);
        }
    }
}
