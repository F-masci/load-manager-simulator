package it.uniroma2.pmcsn.utils.chart;

import it.uniroma2.pmcsn.utils.LogFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Base class for chart utilities, providing common subplot creation, 
 * event marker styling, and file export methods.
 */
public abstract class BaseChartUtility {
    protected static final LogFactory.ModuleLogger logger = LogFactory.getLogger(BaseChartUtility.class, "CHART");

    /**
     * Creates a step-area subplot for discrete states (e.g., active servers, speed).
     */
    protected static XYPlot createAreaSubplot(XYSeries series, String label, Color color) {
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        NumberAxis rangeAxis = new NumberAxis(label);
        XYStepAreaRenderer renderer = new XYStepAreaRenderer(XYStepAreaRenderer.AREA);
        renderer.setSeriesPaint(0, color);
        XYPlot subplot = new XYPlot(dataset, null, rangeAxis, renderer);
        subplot.setBackgroundPaint(Color.WHITE);
        subplot.setRangeGridlinePaint(new Color(230, 230, 230));
        return subplot;
    }

    /**
     * Creates a line subplot for continuous metrics (e.g., response time, utilization).
     */
    protected static XYPlot createLineSubplot(XYSeries series, String label, Color color) {
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

    /**
     * Applies threshold horizontal lines to a plot.
     */
    protected static void applyThresholds(XYPlot plot, double up, double down, String upLabel, String downLabel) {
        Color transparentColor = new Color(0, 0, 0, 0);
        if (up > 0 && upLabel != null) {
            Color outColor = new Color(220, 20, 60);
            ValueMarker upMarker = new ValueMarker(up);
            upMarker.setPaint(outColor);
            upMarker.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10, 5}, 0));
            upMarker.setLabel(upLabel + ": " + String.format("%.2f", up));
            upMarker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
            upMarker.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);

            upMarker.setLabelBackgroundColor(transparentColor);
            upMarker.setLabelPaint(outColor);
            upMarker.setLabelFont(new Font("SansSerif", Font.BOLD, 11));

            plot.addRangeMarker(upMarker);
        }
        if (down > 0 && downLabel != null) {
            Color inColor = new Color(34, 139, 34);
            ValueMarker downMarker = new ValueMarker(down);
            downMarker.setPaint(inColor);
            downMarker.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10, 5}, 0));
            downMarker.setLabel(downLabel + ": " + String.format("%.2f", down));
            downMarker.setLabelAnchor(RectangleAnchor.BOTTOM_RIGHT);
            downMarker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);

            downMarker.setLabelBackgroundColor(transparentColor);
            downMarker.setLabelPaint(inColor);
            downMarker.setLabelFont(new Font("SansSerif", Font.BOLD, 11));

            plot.addRangeMarker(downMarker);
        }
    }

    /**
     * Applies a single threshold horizontal limit line to a plot by reusing applyThresholds.
     */
    protected static void applyLimit(XYPlot plot, double limit, String label, Color color) {
        applyThresholds(plot, limit, 0.0, label, null);
    }

    /**
     * Applies semi-transparent event markers (Arrivals, Completions, Scaling Checks) to a plot.
     */
    protected static void applyEventMarkers(XYPlot plot, XYSeries arrivals, XYSeries completions, XYSeries checks) {
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

    /**
     * Saves the chart as a PNG image.
     */
    protected static void saveChart(JFreeChart chart, String path, int width, int height) {
        try {
            ChartUtils.saveChartAsPNG(new File(path), chart, width, height);
            logger.info("Chart generated: {}", path);
        } catch (IOException e) {
            logger.error("Failed to save chart: {}", path, e);
        }
    }
}
