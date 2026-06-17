package it.uniroma2.pmcsn.utils.chart;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Utility for generating system-level charts, such as load comparisons.
 * Extends BaseChartUtility for shared styling and export logic.
 */
public class SystemChartUtility extends BaseChartUtility {

    /**
     * Generates a comparison chart of web server vs spike server load.
     *
     * @param csvPath path to the source csv file
     * @param outputPath path for the output png image
     */
    public static void generateLoadComparisonChart(String csvPath, String outputPath) {
        XYSeries wsSeries = new XYSeries("Web Server (Active Jobs)");
        XYSeries ssSeries = new XYSeries("Spike Server (Active Jobs)");
        XYSeries totalSeries = new XYSeries("Total System Load");
        XYSeries arrivalSeries = new XYSeries("Arrivals");
        XYSeries completionSeries = new XYSeries("Completions");

        double siMaxVal = 0;

        // Parse load comparison data
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String line = reader.readLine(); 
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
                totalSeries.add(clock, wsJobs + ssJobs);

                // Track discrete workload events for markers
                if ("ARRIVAL".equalsIgnoreCase(event)) {
                    arrivalSeries.add(clock, (double) (wsJobs + ssJobs));
                } else if ("COMPLETION".equalsIgnoreCase(event)) {
                    completionSeries.add(clock, (double) (wsJobs + ssJobs));
                }
            }
        } catch (IOException | NumberFormatException e) {
            logger.error("Error reading simulation state file: {}", csvPath, e);
            return;
        }

        // Initialize Plot with specific axes
        NumberAxis xAxis = new NumberAxis("Time (seconds)");
        NumberAxis yAxis = new NumberAxis("Number of Jobs");
        XYPlot plot = new XYPlot();
        plot.setDomainAxis(xAxis);
        plot.setRangeAxis(yAxis);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setAxisOffset(new RectangleInsets(10, 10, 10, 10));

        // Dataset 0: Area contribution of each server layer
        XYSeriesCollection areaDataset = new XYSeriesCollection();
        areaDataset.addSeries(wsSeries);
        areaDataset.addSeries(ssSeries);
        plot.setDataset(0, areaDataset);
        XYStepAreaRenderer areaRenderer = new XYStepAreaRenderer(XYStepAreaRenderer.AREA);
        areaRenderer.setSeriesPaint(0, new Color(94, 129, 172, 140)); // Nord Blue
        areaRenderer.setSeriesPaint(1, new Color(191, 97, 106, 140)); // Nord Red
        plot.setRenderer(0, areaRenderer);

        // Dataset 1: Total System Load Line (Bold and High-Contrast)
        XYSeriesCollection totalDataset = new XYSeriesCollection(totalSeries);
        plot.setDataset(1, totalDataset);
        XYStepRenderer totalRenderer = new XYStepRenderer();
        totalRenderer.setSeriesPaint(0, new Color(128, 128, 128)); // Light gray for contrast
        totalRenderer.setSeriesStroke(0, new BasicStroke(1.5f)); // Thick line for visibility
        plot.setRenderer(1, totalRenderer);

        // Dataset 2: Event Markers (Hidden from Legend to reduce clutter)
        XYSeriesCollection markerDataset = new XYSeriesCollection();
        markerDataset.addSeries(arrivalSeries);
        markerDataset.addSeries(completionSeries);
        plot.setDataset(2, markerDataset);
        XYLineAndShapeRenderer markerRenderer = new XYLineAndShapeRenderer(false, true);
        markerRenderer.setSeriesVisibleInLegend(0, false);
        markerRenderer.setSeriesVisibleInLegend(1, false);
        
        // Arrivals: Red X
        markerRenderer.setSeriesPaint(0, new Color(191, 97, 106, 180)); 
        markerRenderer.setSeriesShape(0, ShapeUtils.createDiagonalCross(5.0f, 1.5f));
        
        // Completions: Green Circle
        markerRenderer.setSeriesPaint(1, new Color(34, 139, 34, 180)); 
        markerRenderer.setSeriesShape(1, new Ellipse2D.Double(-4, -4, 8, 8));
        plot.setRenderer(2, markerRenderer);

        // Final Plot and Chart Polish
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(230, 230, 230));
        plot.setRangeGridlinePaint(new Color(230, 230, 230));

        // Capacity Limit Line using base utility
        applyLimit(plot, siMaxVal, "SI Max");

        JFreeChart chart = new JFreeChart("System Load Dynamics", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setBackgroundPaint(Color.WHITE);

        saveChart(chart, outputPath, 1200, 800);
    }
}
