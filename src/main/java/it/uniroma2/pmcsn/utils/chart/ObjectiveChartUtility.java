package it.uniroma2.pmcsn.utils.chart;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
     * Generates a bar chart for routing policy comparison (Objective 2.1).
     *
     * @param r0Map        Map of policy names to mean response times.
     * @param stdDevMap    Map of policy names to standard deviations.
     * @param p99Map       Map of policy names to 99th percentiles.
     * @param outputPath   The path to save the generated image.
     * @param slaThreshold The SLA threshold for response time.
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

            XYPlot statePlot = createXYSubplot(bandStateData.get(band), "State Changes", stateRenderer);

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
        for (Double cv : sortedCvs) {
            Map<Integer, XYSeries> wsSeries = cvResults.get(cv);
            for (int n = 1; n <= 5; n++) if (wsSeries.containsKey(n)) globalDataset.addSeries(wsSeries.get(n));
        }
        NumberAxis rangeAxis = new NumberAxis("Avg R0 (s)");
        rangeAxis.setRange(0.0, 60.0);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        for (int i = 0; i < globalDataset.getSeriesCount(); i++) { 
            renderer.setSeriesPaint(i, PALETTE[i % PALETTE.length]); 
            renderer.setSeriesStroke(i, new BasicStroke(2.0f)); 
        }
        mainPlot.setDataset(globalDataset); mainPlot.setRenderer(renderer); mainPlot.setRangeAxis(rangeAxis);
        setupPlot(mainPlot); applyLimit(mainPlot, slaThreshold, "SLA");
        JFreeChart chart = new JFreeChart("Horizontal Scaling Limits (1.3)", mainPlot);
        chart.setBackgroundPaint(Color.WHITE);
        saveChart(chart, outputPath, 1000, 700);
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
     * Generates a grid of charts for transient analysis (Objective 4.2).
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
                    result.add(new LegendItem("State Changes", null, null, null, new Line2D.Double(0, 0, 10, 0), new BasicStroke(2.0f), NORD_RED));
                    result.add(new LegendItem("Avg Response Time R0", null, null, null, new Line2D.Double(0, 0, 10, 0), new BasicStroke(2.0f), NORD_BLUE));
                    for (int i = 0; i < series.getSeriesCount(); i++) {
                        result.add(new LegendItem(series.getSeriesKey(i).toString(), null, null, null, shapes[i % shapes.length], Color.DARK_GRAY));
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

