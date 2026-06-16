package it.uniroma2.pmcsn.objective;

import it.uniroma2.pmcsn.LoadManagerSimulator;
import it.uniroma2.pmcsn.configs.ApplicationConfig;
import it.uniroma2.pmcsn.facade.SimulationFacade;
import it.uniroma2.pmcsn.utils.LogFactory;
import it.uniroma2.pmcsn.utils.objective.ObjectiveUtils;
import it.uniroma2.pmcsn.utils.chart.ObjectiveChartUtility;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Objective 4.1: Vertical Scaler Stability Analysis
 * Analyzes the impact of different bands (siMax - siLow) on system stability
 * (measured by state changes) and performance across varying arrival rates.
 */
public class VerticalScalerStabilityObjective extends LoadManagerSimulator {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(VerticalScalerStabilityObjective.class, "OBJ4.1");

    public static void main(String[] args) {
        new VerticalScalerStabilityObjective().start(args);
    }

    @Override
    protected void run(ApplicationConfig config) {
        logger.info("Starting Vertical Scaler Stability Objective (4.1)...");

        int siMax = config.load().siMax();
        int[] bands = {0, 5, 10, 20};
        double[] cvs = {1.0, 4.0, 10.0};
        double[] lambdas = {1.0, 2.5, 5.0, 7.5, 10.0, 15.0};

        StringBuilder report = new StringBuilder();
        StringBuilder csv = new StringBuilder("Band,CV,Lambda,State_Changes,R0\n");
        
        Map<Integer, XYSeriesCollection> bandStateChangesData = new LinkedHashMap<>();
        Map<Integer, XYSeriesCollection> bandRtData = new LinkedHashMap<>();

        report.append("\nVertical Scaler Stability (Objective 4.1) Report\n");
        report.append(String.format("%-15s | %-5s | %-10s | %-15s | %-10s\n", "Band", "CV", "Lambda", "State Changes", "R0"));
        report.append("--------------------------------------------------------------------------------\n");

        for (int band : bands) {
            int siLow = Math.max(0, siMax - band);
            
            XYSeriesCollection stateDataset = new XYSeriesCollection();
            XYSeriesCollection rtDataset = new XYSeriesCollection();
            
            bandStateChangesData.put(band, stateDataset);
            bandRtData.put(band, rtDataset);

            for (double cv : cvs) {
                XYSeries stateSeries = new XYSeries("CV=" + cv);
                XYSeries rtSeries = new XYSeries("CV=" + cv);

                for (double lambda : lambdas) {
                    ApplicationConfig currentConfig = new ApplicationConfig(
                            new ApplicationConfig.LoadConfig(
                                    1.0 / lambda, cv,
                                    ApplicationConfig.MEAN_SERVICE, ApplicationConfig.CV_SERVICE,
                                    siMax, siLow,
                                    ApplicationConfig.ROUTING_POLICY, ApplicationConfig.WORKLOAD_TYPE, null
                            ),
                            config.cluster(),
                            config.scaling(),
                            config.execution(),
                            config.logging()
                    );

                    SimulationFacade facade = new SimulationFacade(currentConfig);
                    SimulationFacade.AggregatedResults results = facade.runSimulation();

                    double diverted = results.divertedJobs().mean();
                    double r0 = results.responseTime().mean();

                    report.append(String.format("%-15d | %-5.1f | %-10.1f | %-15.1f | %-10.4f\n", band, cv, lambda, diverted, r0));
                    csv.append(String.format("%d,%.1f,%.1f,%.1f,%.4f\n", band, cv, lambda, diverted, r0));
                    
                    stateSeries.add(lambda, diverted);
                    rtSeries.add(lambda, r0);
                }
                stateDataset.addSeries(stateSeries);
                rtDataset.addSeries(rtSeries);
            }
            logger.info("Completed analysis for Band = {}", band);
        }

        logger.info(report.toString());
        ObjectiveUtils.saveToCsv("vertical_scaler_stability.csv", csv.toString());
        ObjectiveChartUtility.generateVerticalScalerStabilityGrid(bandStateChangesData, bandRtData, "data/objective/vertical_scaler_stability.png", 5.0);
    }
}
