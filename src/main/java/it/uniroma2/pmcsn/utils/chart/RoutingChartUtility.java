package it.uniroma2.pmcsn.utils.chart;

import it.uniroma2.pmcsn.model.load.routing.RoutingPolicy;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for generating routing balance and load distribution charts.
 * Extends BaseChartUtility for subplot creation and export.
 */
public class RoutingChartUtility extends BaseChartUtility {

    /**
     * Generates a chart showing individual load for each active Web Server.
     * Useful for validating that the load balancer is properly distributing jobs.
     *
     * @param csvPath Path to the CSV source file.
     * @param outputPath Path for the generated PNG.
     */
    public static void generateRoutingBalanceChart(RoutingPolicy policy, String csvPath, String outputPath) {
        Map<Integer, XYSeries> serverSeriesMap = new HashMap<>();

        // Parse individual server loads from CSV
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String header = reader.readLine();
            if (header == null) return;
            String[] headers = header.split(",");
            
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].startsWith("server_")) {
                    serverSeriesMap.put(i, new XYSeries(headers[i]));
                }
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                double clock = Double.parseDouble(parts[0]);
                for (Map.Entry<Integer, XYSeries> entry : serverSeriesMap.entrySet()) {
                    entry.getValue().add(clock, Double.parseDouble(parts[entry.getKey()]));
                }
            }
        } catch (IOException | NumberFormatException e) {
            logger.error("Error reading routing balance file: {}", csvPath, e);
            return;
        }

        // Create a vertically stacked chart for comparison
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(new NumberAxis("Time (seconds)"));
        combinedPlot.setGap(10.0);
        combinedPlot.setOrientation(PlotOrientation.VERTICAL);

        // Color palette for servers
        Color[] colors = {
            new Color(94, 129, 172), new Color(191, 97, 106), new Color(163, 190, 140),
            new Color(208, 135, 112), new Color(180, 142, 173), new Color(235, 203, 139)
        };

        int colorIdx = 0;
        for (XYSeries series : serverSeriesMap.values()) {
            XYPlot subplot = createRoutingSubplot(series, colors[colorIdx++ % colors.length]);
            combinedPlot.add(subplot, 1);
        }

        JFreeChart chart = new JFreeChart("Routing Policy Distribution: " + policy, JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, true);
        chart.setBackgroundPaint(Color.WHITE);

        // Dynamic height based on number of servers
        int height = Math.max(800, serverSeriesMap.size() * 250);
        saveChart(chart, outputPath, 1200, height);
    }

    /**
     * Helper to create sub-plots for routing balance.
     */
    private static XYPlot createRoutingSubplot(XYSeries series, Color color) {
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        NumberAxis rangeAxis = new NumberAxis("Jobs (" + series.getKey() + ")");
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        XYStepAreaRenderer renderer = new XYStepAreaRenderer(XYStepAreaRenderer.AREA);
        renderer.setSeriesPaint(0, color);
        XYPlot subplot = new XYPlot(dataset, null, rangeAxis, renderer);
        subplot.setBackgroundPaint(Color.WHITE);
        subplot.setRangeGridlinePaint(new Color(230, 230, 230));
        return subplot;
    }
}
