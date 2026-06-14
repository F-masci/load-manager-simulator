package it.uniroma2.pmcsn.utils.chart;

import it.uniroma2.pmcsn.utils.LogFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Utility for generating system-level charts, such as load comparisons.
 */
public class SystemChartUtility {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(SystemChartUtility.class, "CHART");

    /**
     * Generates a comparison chart of Web Server vs Spike Server load.
     * Marks job diversions to the Spike Server.
     */
    public static void generateLoadComparisonChart(String csvPath, String outputPath) {
        XYSeries wsSeries = new XYSeries("Web Server (Active Jobs)");
        XYSeries ssSeries = new XYSeries("Spike Server (Overflow)");
        XYSeries diversionSeries = new XYSeries("Diverted Job Start");
        XYSeries arrivalSeries = new XYSeries("Arrivals");
        XYSeries completionSeries = new XYSeries("Completions");

        double siMaxVal = 0;
        int lastSsJobs = 0;

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
                siMaxVal = Double.parseDouble(parts[6]);

                wsSeries.add(clock, wsJobs);
                ssSeries.add(clock, ssJobs);

                if ("ARRIVAL".equalsIgnoreCase(event)) {
                    arrivalSeries.add(clock, (double) (wsJobs + ssJobs));
                    if (ssJobs > lastSsJobs) {
                        diversionSeries.add(clock, (double) ssJobs);
                    }
                } else if ("COMPLETION".equalsIgnoreCase(event)) {
                    completionSeries.add(clock, (double) (wsJobs + ssJobs));
                }
                
                lastSsJobs = ssJobs;
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
        stepRenderer.setSeriesPaint(0, new Color(94, 129, 172, 150));  // Blue
        stepRenderer.setSeriesPaint(1, new Color(191, 97, 106, 150));  // Red
        plot.setRenderer(0, stepRenderer);

        // Limit Marker
        ValueMarker thresholdMarker = new ValueMarker(siMaxVal);
        thresholdMarker.setPaint(new Color(220, 20, 60));
        thresholdMarker.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{15, 5}, 0));
        thresholdMarker.setLabel("LIMIT (SI Max = " + (int)siMaxVal + ")");
        plot.addRangeMarker(thresholdMarker);

        // Event Markers (Arrivals, Completions, Diversions)
        XYSeriesCollection eventDataset = new XYSeriesCollection();
        eventDataset.addSeries(arrivalSeries);
        eventDataset.addSeries(completionSeries);
        eventDataset.addSeries(diversionSeries);
        
        plot.setDataset(1, eventDataset);
        XYLineAndShapeRenderer eventRenderer = new XYLineAndShapeRenderer(false, true);
        
        // Arrivals: Red X (Alpha 0.7)
        eventRenderer.setSeriesPaint(0, new Color(191, 97, 106, 180));
        eventRenderer.setSeriesShape(0, org.jfree.chart.util.ShapeUtils.createDiagonalCross(4.0f, 1.2f));
        
        // Completions: Green Dots (Alpha 0.7)
        eventRenderer.setSeriesPaint(1, new Color(34, 139, 34, 180));
        eventRenderer.setSeriesShape(1, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));
        
        // Diversions: Green Diamonds (Alpha 0.8 for visibility)
        eventRenderer.setSeriesPaint(2, new Color(163, 190, 140, 210));
        eventRenderer.setSeriesShape(2, new java.awt.geom.Ellipse2D.Double(-5, -5, 10, 10)); 
        
        plot.setRenderer(1, eventRenderer);

        try {
            ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1200, 800);
            logger.info("Load comparison chart generated at: {}", outputPath);
        } catch (IOException e) {
            logger.error("Failed to save chart image: {}", outputPath, e);
        }
    }
}
