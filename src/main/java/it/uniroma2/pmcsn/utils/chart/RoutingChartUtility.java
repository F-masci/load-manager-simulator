package it.uniroma2.pmcsn.utils.chart;

import it.uniroma2.pmcsn.utils.LogFactory;
import org.jfree.chart.ChartUtils;
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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for generating routing balance and load distribution charts.
 */
public class RoutingChartUtility {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(RoutingChartUtility.class, "CHART");

    /**
     * Generates a chart showing individual load for each active Web Server.
     */
    public static void generateRoutingBalanceChart(String csvPath, String outputPath) {
        Map<Integer, XYSeries> serverSeriesMap = new HashMap<>();

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

        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(new NumberAxis("Time (seconds)"));
        combinedPlot.setGap(10.0);
        combinedPlot.setOrientation(PlotOrientation.VERTICAL);

        Color[] colors = {
            new Color(94, 129, 172), new Color(191, 97, 106), new Color(163, 190, 140),
            new Color(208, 135, 112), new Color(180, 142, 173), new Color(235, 203, 139)
        };

        int colorIdx = 0;
        for (XYSeries series : serverSeriesMap.values()) {
            XYPlot subplot = createSubplot(series, colors[colorIdx++ % colors.length]);
            combinedPlot.add(subplot, 1);
        }

        JFreeChart chart = new JFreeChart("Routing Balance Validation", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, true);
        chart.setBackgroundPaint(Color.WHITE);

        try {
            int height = Math.max(800, serverSeriesMap.size() * 250);
            ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1200, height);
            logger.info("Routing balance chart generated: {}", outputPath);
        } catch (IOException e) {
            logger.error("Failed to save routing chart: {}", outputPath, e);
        }
    }

    private static XYPlot createSubplot(XYSeries series, Color color) {
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
