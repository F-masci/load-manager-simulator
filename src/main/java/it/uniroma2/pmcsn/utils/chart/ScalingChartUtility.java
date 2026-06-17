package it.uniroma2.pmcsn.utils.chart;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for generating scaling dynamics charts (Horizontal and Vertical).
 * Extends BaseChartUtility for common plotting logic.
 */
public class ScalingChartUtility extends BaseChartUtility {

    /**
     * Generates a horizontal scaling dynamics chart.
     *
     * @param csvPath path to the source csv file
     * @param outputPath path for the output png image
     */
    public static void generateHorizontalScalingChart(String csvPath, String outputPath) {
        XYSeries activeServersSeries = new XYSeries("Active Web Servers");
        XYSeries hMetricSeries = new XYSeries("Windowed Response Time");
        XYSeries cooldownSeries = new XYSeries("Residual Cooldown");
        XYSeries arrivalSeries = new XYSeries("Arrivals");
        XYSeries completionSeries = new XYSeries("Completions");
        XYSeries checkSeries = new XYSeries("Scaling Checks");
        
        double scaleOutLimit = 0;
        double scaleInLimit = 0;
        List<Double> scalingActionTimes = new ArrayList<>();
        int lastActiveServers = -1;

        // Parse CSV data for horizontal scaling
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
                scaleOutLimit = Double.parseDouble(parts[5]);
                scaleInLimit = Double.parseDouble(parts[6]);
                double hCooldown = Double.parseDouble(parts[7]);

                activeServersSeries.add(clock, activeWebServers);
                hMetricSeries.add(clock, hMetric);
                cooldownSeries.add(clock, hCooldown);

                // Filter discrete events
                switch (event.toUpperCase()) {
                    case "ARRIVAL" -> arrivalSeries.add(clock, hMetric);
                    case "COMPLETION" -> completionSeries.add(clock, hMetric);
                }
                
                // Filter scaler-specific check markers
                if ("HORIZONTAL".equalsIgnoreCase(check) || "BOTH".equalsIgnoreCase(check)) {
                    checkSeries.add(clock, hMetric);
                }

                // Detect exact moments of scaling action
                if (lastActiveServers != -1 && activeWebServers != lastActiveServers) {
                    scalingActionTimes.add(clock);
                }
                lastActiveServers = activeWebServers;
            }
        } catch (IOException | NumberFormatException e) {
            logger.error("Error reading horizontal scaling file: {}", csvPath, e);
            return;
        }

        // Create subplots using base utilities
        XYPlot subplot1 = createAreaSubplot(activeServersSeries, "Active Servers", new Color(94, 129, 172, 120));
        XYPlot subplot2 = createLineSubplot(hMetricSeries, "Avg Response Time (seconds)", new Color(191, 97, 106));
        XYPlot subplot3 = createLineSubplot(cooldownSeries, "Cooldown (seconds)", new Color(136, 192, 208));

        // Annotate subplot with thresholds and markers
        applyThresholds(subplot2, scaleOutLimit, scaleInLimit, "Scale OUT", "Scale IN");
        applyEventMarkers(subplot2, arrivalSeries, completionSeries, checkSeries);

        // Add vertical lines at scaling events
        for (double t : scalingActionTimes) {
            ValueMarker marker = new ValueMarker(t);
            marker.setPaint(new Color(76, 86, 106, 180));
            marker.setStroke(new BasicStroke(0.7f));
            subplot1.addDomainMarker(marker);
            subplot2.addDomainMarker(marker);
        }

        // Combine into a single vertical stack
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

    /**
     * Generates a vertical scaling dynamics chart.
     *
     * @param csvPath path to the source csv file
     * @param outputPath path for the output png image
     */
    public static void generateVerticalScalingChart(String csvPath, String outputPath) {
        XYSeries spikeSpeedSeries = new XYSeries("Spike Server Speed Multiplier");
        XYSeries vMetricSeries = new XYSeries("Job in Spike Server");
        XYSeries cooldownSeries = new XYSeries("Residual Cooldown");
        XYSeries arrivalSeries = new XYSeries("Arrivals");
        XYSeries completionSeries = new XYSeries("Completions");
        XYSeries checkSeries = new XYSeries("Scaling Checks");
        
        double vUpperLimit = 0;
        double vLowerLimit = 0;
        List<Double> speedActionTimes = new ArrayList<>();
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
        XYPlot subplot2 = createLineSubplot(vMetricSeries, "Jobs", new Color(208, 135, 112));
        XYPlot subplot3 = createLineSubplot(cooldownSeries, "Cooldown (seconds)", new Color(180, 142, 173));

        applyThresholds(subplot2, vUpperLimit, vLowerLimit, "Speed UP", "Speed DOWN");
        applyEventMarkers(subplot2, arrivalSeries, completionSeries, checkSeries);

        for (double t : speedActionTimes) {
            ValueMarker marker = new ValueMarker(t);
            marker.setPaint(new Color(76, 86, 106, 180));
            marker.setStroke(new BasicStroke(0.7f));
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
}
