package it.uniroma2.pmcsn.utils.chart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
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

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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

                if (clock > 300.0) continue;

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
                "System Load Dynamics: Web Server vs Spike Server (300s window)",
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

        // Axis Zoom and Formatting
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setRange(0, 300); 
        
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(0, Math.max(10, maxJobsSeen + 2));
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        try {
            ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1200, 800);
            logger.info("Enhanced focused load comparison chart generated at: {}", outputPath);
        } catch (IOException e) {
            logger.error("Failed to save chart image: {}", outputPath, e);
        }
    }
}
