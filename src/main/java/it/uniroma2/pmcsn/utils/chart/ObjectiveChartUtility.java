package it.uniroma2.pmcsn.utils.chart;

import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.*;
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
import java.awt.*;
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
 */
public class ObjectiveChartUtility extends BaseChartUtility {

    /**
     * Objective 1.1: SI_max Estimation Stacked Chart.
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
        rtPlot.setBackgroundPaint(Color.WHITE);
        rtPlot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        applyLimit(rtPlot, slaThreshold, "SLA");

        XYSeriesCollection countDataset = new XYSeriesCollection(spikeCount);
        NumberAxis countAxis = new NumberAxis("Diverted Jobs (count)");
        XYLineAndShapeRenderer countRenderer = new XYLineAndShapeRenderer(true, true);
        countRenderer.setSeriesPaint(0, new Color(191, 97, 106));

        XYPlot spikePlot = new XYPlot(countDataset, null, countAxis, countRenderer);
        spikePlot.setBackgroundPaint(Color.WHITE);
        spikePlot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        NumberAxis utilAxis = new NumberAxis("Spike Utilization (%)");
        utilAxis.setRange(0, 105);
        spikePlot.setRangeAxis(1, utilAxis);
        spikePlot.setDataset(1, new XYSeriesCollection(spikeUtil));
        spikePlot.mapDatasetToRangeAxis(1, 1);

        XYLineAndShapeRenderer utilRenderer = new XYLineAndShapeRenderer(true, true);
        utilRenderer.setSeriesPaint(0, new Color(136, 192, 208));
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
     * Objective 1.2: Vertical Step Sizing Stacked Chart.
     */
    public static void generateVerticalSizingStackedChart(
            XYSeries rtSeries, XYSeries speedSeries, XYSeries utilSeries,
            String outputPath, double slaThreshold) {

        XYPlot rtPlot = createLineSubplot(rtSeries, "Response Time (s)", Color.BLUE);
        applyLimit(rtPlot, slaThreshold, "SLA");
        XYPlot speedPlot = createLineSubplot(speedSeries, "Avg Speed Mult", new Color(191, 97, 106));
        XYPlot utilPlot = createLineSubplot(utilSeries, "Utilization (%)", new Color(143, 188, 187));

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
     * Objective 2.1: Routing Policy Performance Analysis Chart.
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
        p99Renderer.setSeriesPaint(0, new Color(191, 97, 106));
        p99Renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        p99Renderer.setSeriesShape(0, new java.awt.geom.Rectangle2D.Double(-4, -4, 8, 8));
        plot.setDataset(1, lineDataset);
        plot.setRenderer(1, p99Renderer);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        applyLimitCategory(plot, slaThreshold, "SLA");

        JFreeChart chart = new JFreeChart("Routing Analysis (2.1)", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setBackgroundPaint(Color.WHITE);
        saveChart(chart, outputPath, 1000, 700);
    }

    private static class ClippedColoredStatisticalBarRenderer extends StatisticalBarRenderer {
        private final Color[] colors = { new Color(94, 129, 172), new Color(191, 97, 106), new Color(163, 190, 140), new Color(208, 135, 112), new Color(180, 142, 173) };
        @Override public Paint getItemPaint(int r, int c) { return colors[c % colors.length]; }
    }

    /**
     * Objective 3.1: Cost & R0 vs Arrival Rate Grid (by Cooldown).
     */
    public static void generateCostCooldownGrid(
            Map<Double, XYSeriesCollection> costData,
            Map<Double, XYSeriesCollection> rtData,
            String outputPath, double slaThreshold) {

        List<Double> sortedCooldowns = new ArrayList<>(costData.keySet());
        Collections.sort(sortedCooldowns);
        XYLineAndShapeRenderer sharedRenderer = createCustomRenderer();

        int width = 1200;
        int height = 1000;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        String title = "Economic & Performance Analysis (3.1)";
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (width - titleWidth) / 2, 35);

        int cellW = width / 2;
        int cellH = (height - 100) / 2;

        int idx = 0;
        for (Double cd : sortedCooldowns) {
            NumberAxis domainAxis = new NumberAxis("Lambda [CD=" + cd + "s]");
            CombinedDomainXYPlot subplot = new CombinedDomainXYPlot(domainAxis);
            subplot.setGap(10.0);

            NumberAxis costAxis = new NumberAxis("Cost");
            costAxis.setAutoRangeIncludesZero(false);
            XYPlot costPlot = new XYPlot(costData.get(cd), null, costAxis, sharedRenderer);
            costPlot.setBackgroundPaint(Color.WHITE);

            NumberAxis rtAxis = new NumberAxis("R0 (s)");
            rtAxis.setAutoRangeIncludesZero(false);
            XYPlot rtPlot = new XYPlot(rtData.get(cd), null, rtAxis, sharedRenderer);
            rtPlot.setBackgroundPaint(Color.WHITE);
            applyLimit(rtPlot, slaThreshold, "SLA");

            subplot.add(costPlot, 1);
            subplot.add(rtPlot, 1);

            boolean showLegend = (idx == 1);
            JFreeChart subchart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, subplot, showLegend);
            subchart.setBackgroundPaint(Color.WHITE);

            if (showLegend) {
                LegendTitle legend = subchart.getLegend();
                if (legend != null) {
                    legend.setSources(new LegendItemSource[] {
                        () -> {
                            LegendItemCollection result = new LegendItemCollection();
                            XYSeriesCollection first = costData.get(sortedCooldowns.get(0));
                            for (int i = 0; i < first.getSeriesCount(); i++) result.add(sharedRenderer.getLegendItem(0, i));
                            return result;
                        }
                    });
                }
            }

            int x = (idx % 2) * cellW;
            int y = 60 + (idx / 2) * cellH;
            subchart.draw(g2, new Rectangle(x, y, cellW, cellH));
            idx++;
        }
        g2.dispose();

        try {
            ImageIO.write(img, "png", new File(outputPath));
            logger.info("Chart generated: {}", outputPath);
        } catch (IOException e) {
            logger.error("Failed to save chart: {}", outputPath, e);
        }
    }

    /**
     * Objective 4.1: Vertical Scaler Stability & R0 vs Arrival Rate Grid.
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

        int width = 1200;
        int height = 1000;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        String title = "Vertical Scaler Stability & Performance (4.1)";
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (width - titleWidth) / 2, 35);

        int cellW = width / 2;
        int cellH = (height - 100) / 2;

        int idx = 0;
        for (Integer band : sortedBands) {
            NumberAxis domainAxis = new NumberAxis("Lambda [Band=" + band + "]");
            CombinedDomainXYPlot subplot = new CombinedDomainXYPlot(domainAxis);
            subplot.setGap(10.0);

            // State Changes Renderer (Reddish)
            XYLineAndShapeRenderer stateRenderer = new XYLineAndShapeRenderer(true, true);
            for (int i = 0; i < bandStateData.get(band).getSeriesCount(); i++) {
                stateRenderer.setSeriesPaint(i, new Color(191, 97, 106)); // Nord Red
                stateRenderer.setSeriesStroke(i, new BasicStroke(2.0f));
                stateRenderer.setSeriesShape(i, shapes[i % shapes.length]);
            }

            NumberAxis stateAxis = new NumberAxis("State Changes");
            XYPlot statePlot = new XYPlot(bandStateData.get(band), null, stateAxis, stateRenderer);
            statePlot.setBackgroundPaint(Color.WHITE);

            // RT Renderer (Bluish)
            XYLineAndShapeRenderer rtRenderer = new XYLineAndShapeRenderer(true, true);
            for (int i = 0; i < bandRtData.get(band).getSeriesCount(); i++) {
                rtRenderer.setSeriesPaint(i, new Color(94, 129, 172)); // Nord Blue
                rtRenderer.setSeriesStroke(i, new BasicStroke(2.0f));
                rtRenderer.setSeriesShape(i, shapes[i % shapes.length]);
            }

            NumberAxis rtAxis = new NumberAxis("R0 (s)");
            rtAxis.setAutoRangeIncludesZero(false);
            XYPlot rtPlot = new XYPlot(bandRtData.get(band), null, rtAxis, rtRenderer);
            rtPlot.setBackgroundPaint(Color.WHITE);
            applyLimit(rtPlot, slaThreshold, "SLA");

            subplot.add(statePlot, 1);
            subplot.add(rtPlot, 1);

            boolean showLegend = (idx == 1);
            JFreeChart subchart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, subplot, showLegend);
            subchart.setBackgroundPaint(Color.WHITE);

            if (showLegend) {
                LegendTitle legend = subchart.getLegend();
                if (legend != null) {
                    legend.setSources(new LegendItemSource[] {
                        () -> {
                            LegendItemCollection result = new LegendItemCollection();
                            // Color Legend
                            result.add(new LegendItem("State Changes", null, null, null,
                                    new Line2D.Double(0, 0, 10, 0), new BasicStroke(2.0f), new Color(191, 97, 106)));
                            result.add(new LegendItem("Avg Response Time R0", null, null, null,
                                    new Line2D.Double(0, 0, 10, 0), new BasicStroke(2.0f), new Color(94, 129, 172)));

                            // Shape Legend (CVs)
                            XYSeriesCollection first = bandStateData.get(sortedBands.get(0));
                            for (int i = 0; i < first.getSeriesCount(); i++) {
                                String cvLabel = first.getSeriesKey(i).toString();
                                result.add(new LegendItem(cvLabel, null, null, null,
                                        shapes[i % shapes.length], Color.DARK_GRAY));
                            }
                            return result;
                        }
                    });
                }
            }

            int x = (idx % 2) * cellW;
            int y = 60 + (idx / 2) * cellH;
            subchart.draw(g2, new Rectangle(x, y, cellW, cellH));
            idx++;
        }
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
        Color[] colors = { new Color(94, 129, 172), new Color(191, 97, 106), new Color(163, 190, 140), new Color(208, 135, 112), new Color(180, 142, 173) };
        for (int i = 0; i < colors.length; i++) { renderer.setSeriesPaint(i, colors[i]); renderer.setSeriesStroke(i, new BasicStroke(2.0f)); }
        return renderer;
    }

    /**
     * Objective 1.3: Horizontal Scaler Parameter Estimation.
     */
    public static void generateHorizontalParameterEstimationChart(
            Map<Double, Map<Integer, XYSeries>> cvResults, String outputPath, double slaThreshold) {
        XYPlot mainPlot = new XYPlot();
        mainPlot.setDomainAxis(new NumberAxis("Arrival Rate (lambda)"));
        List<Double> sortedCvs = new ArrayList<>(cvResults.keySet());
        Collections.sort(sortedCvs);
        Color[] wsColors = { new Color(94, 129, 172), new Color(163, 190, 140), new Color(235, 203, 139), new Color(208, 135, 112), new Color(191, 97, 106) };
        XYSeriesCollection globalDataset = new XYSeriesCollection();
        for (Double cv : sortedCvs) {
            Map<Integer, XYSeries> wsSeries = cvResults.get(cv);
            for (int n = 1; n <= 5; n++) if (wsSeries.containsKey(n)) globalDataset.addSeries(wsSeries.get(n));
        }
        NumberAxis rangeAxis = new NumberAxis("Avg R0 (s)");
        rangeAxis.setRange(0.0, 60.0);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        for (int i = 0; i < globalDataset.getSeriesCount(); i++) { renderer.setSeriesPaint(i, wsColors[i % wsColors.length]); renderer.setSeriesStroke(i, new BasicStroke(2.0f)); }
        mainPlot.setDataset(globalDataset); mainPlot.setRenderer(renderer); mainPlot.setRangeAxis(rangeAxis);
        mainPlot.setBackgroundPaint(Color.WHITE); applyLimit(mainPlot, slaThreshold, "SLA");
        JFreeChart chart = new JFreeChart("Horizontal Scaling Limits (1.3)", mainPlot);
        chart.setBackgroundPaint(Color.WHITE);
        saveChart(chart, outputPath, 1000, 700);
    }

    /**
     * Objective 4.3: Burstiness Robustness Line Chart.
     */
    public static void generateBurstinessLineChart(XYSeriesCollection dataset, String outputPath, double slaThreshold) {
        JFreeChart chart = ChartFactory.createXYLineChart("Robustness to Burstiness (4.3)", "Arrival Rate", "R0 (s)", dataset, PlotOrientation.VERTICAL, true, true, false);
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        XYLineAndShapeRenderer renderer = createCustomRenderer();
        plot.setRenderer(renderer);

        applyLimit(plot, slaThreshold, "SLA");
        saveChart(chart, outputPath, 1000, 600);
    }

    /**
     * Objective 4.2: Transient Analysis Grid (2x2).
     * Compares 4 architectures, plotting multiple replications per architecture.
     * @param archData Map of ArchitectureLabel -> XYSeriesCollection (10 series per architecture)
     */
    public static void generateTransientGrid(
            Map<String, XYSeriesCollection> archData, String title, String outputPath, double slaThreshold) {

        List<String> architectures = new ArrayList<>(archData.keySet());

        Color[] runColors = {
            new Color(191, 97, 106),  // Red
            new Color(208, 135, 112), // Orange
            new Color(235, 203, 139), // Yellow
            new Color(163, 190, 140), // Green
            new Color(180, 142, 173), // Purple
            new Color(94, 129, 172),  // Blue
            new Color(136, 192, 208), // Light Blue
            new Color(143, 188, 187), // Cyan
            new Color(76, 86, 106),   // Dark Grey
            new Color(105, 105, 105)  // Grey
        };

        int width = 1200;
        int height = 1000;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (width - titleWidth) / 2, 35);

        int cellW = width / 2;
        int cellH = (height - 50) / 2;

        for (int archIdx = 0; archIdx < architectures.size(); archIdx++) {
            String archName = architectures.get(archIdx);
            XYSeriesCollection dataset = archData.get(archName);

            NumberAxis xAxis = new NumberAxis("Time (s)");
            NumberAxis yAxis = new NumberAxis("R0 (s)");
            yAxis.setAutoRangeIncludesZero(true);

            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false); // Lines only

            // Set different colors for different runs
            for (int i = 0; i < dataset.getSeriesCount(); i++) {
                renderer.setSeriesPaint(i, runColors[i % runColors.length]);
                renderer.setSeriesStroke(i, new BasicStroke(1.5f));
            }

            XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
            plot.setBackgroundPaint(Color.WHITE);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
            applyLimit(plot, slaThreshold, "SLA");

            JFreeChart subchart = new JFreeChart(archName, new Font("SansSerif", Font.BOLD, 16), plot, false);
            subchart.setBackgroundPaint(Color.WHITE);

            int x = (archIdx % 2) * cellW;
            int y = 50 + (archIdx / 2) * cellH;
            subchart.draw(g2, new Rectangle(x, y, cellW, cellH));
        }

        // --- Draw Legend Manually ---
        // Get the first dataset to extract the series names (which contain the seeds)
        XYSeriesCollection sampleDataset = archData.get(architectures.get(0));

        int legendY = height - 80;
        int legendX = 50;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));

        for (int i = 0; i < sampleDataset.getSeriesCount(); i++) {
            g2.setColor(runColors[i % runColors.length]);
            g2.fillRect(legendX, legendY, 20, 10);

            g2.setColor(Color.BLACK);
            String seriesKey = sampleDataset.getSeriesKey(i).toString();
            g2.drawString(seriesKey, legendX + 25, legendY + 10);

            legendX += 200; // Move horizontally
            if (legendX > width - 200) {
                legendX = 50;
                legendY += 20; // Move to next line
            }
        }

        g2.dispose();

        try {
            ImageIO.write(img, "png", new java.io.File(outputPath));
            logger.info("Chart generated: {}", outputPath);
        } catch (IOException e) {
            logger.error("Failed to save chart: {}", outputPath, e);
        }
    }
}
