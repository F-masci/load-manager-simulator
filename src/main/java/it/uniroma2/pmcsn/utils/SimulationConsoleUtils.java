package it.uniroma2.pmcsn.utils;

/**
 * Utility class for console-based simulation reports and progress tracking.
 */
public class SimulationConsoleUtils {

    /**
     * Prints a progress bar for job completion.
     *
     * @param percentage the completion percentage
     * @param currentJobs the number of jobs completed
     * @param currentTime the current simulation clock
     */
    public static void printJobProgressBar(int percentage, long currentJobs, double currentTime) {
        String info = String.format("Jobs: %d | Clock: %.2fs", currentJobs, currentTime);
        printBar(percentage, info);
    }

    /**
     * Prints a progress bar for batch execution.
     *
     * @param currentBatch the current batch index
     * @param totalBatches the total number of batches
     */
    public static void printBatchProgressBar(int currentBatch, int totalBatches) {
        int percentage = (int) ((double) currentBatch / totalBatches * 100);
        String info = String.format("Batch: %d/%d", currentBatch, totalBatches);
        printBar(percentage, info);
    }

    /**
     * Prints a progress bar to the console.
     *
     * @param percentage the percentage to display
     * @param info additional information string
     */
    private static void printBar(int percentage, String info) {
        int barLength = 50;
        int filledLength = (int) (barLength * percentage / 100.0);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                bar.append("=");
            } else if (i == filledLength) {
                bar.append(">");
            } else {
                bar.append(" ");
            }
        }
        bar.append("]");

        String output = String.format("\r%s %3d%% | %s", bar.toString(), percentage, info);
        System.out.print(output);
    }
}
