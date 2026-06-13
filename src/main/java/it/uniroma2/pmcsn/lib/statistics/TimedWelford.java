package it.uniroma2.pmcsn.lib.statistics;

/**
 * An implementation of a time-weighted Welford's algorithm.
 * This class tracks the running mean and variance of a continuous-time variable,
 * where each value is weighted by the duration it persists.
 */
public class TimedWelford {
    private double totalDuration = 0.0;
    private double mean = 0.0;
    private double sumOfSquares = 0.0;

    // For tracking state over time
    private double lastTime = 0.0;
    private double currentValue = 0.0;
    private boolean initialized = false;

    /**
     * Updates the statistic with a value and the duration it persisted.
     *
     * @param value    the value of the statistic
     * @param duration the duration for which the value persisted
     */
    public void update(double value, double duration) {
        if (duration <= 0.0) {
            return;
        }
        double delta = value - mean;
        totalDuration += duration;
        mean += (duration / totalDuration) * delta;
        sumOfSquares += duration * delta * (value - mean);
    }

    /**
     * Updates the statistic by transitioning to a new value at the current simulation time.
     * This automatically calculates the duration since the last update using the previous value.
     *
     * @param currentTime the current simulation clock time
     * @param newValue    the new value of the statistic starting from this time
     */
    public void updateToTime(double currentTime, double newValue) {
        if (!initialized) {
            lastTime = currentTime;
            currentValue = newValue;
            initialized = true;
            return;
        }
        double duration = currentTime - lastTime;
        if (duration > 0.0) {
            update(currentValue, duration);
        }
        currentValue = newValue;
        lastTime = currentTime;
    }

    /**
     * Gets the running time-weighted mean.
     *
     * @return the running mean, or 0.0 if total duration is 0
     */
    public double getMean() {
        return mean;
    }

    /**
     * Gets the running time-weighted variance (population variance, representing the
     * time-average variance over the total duration).
     *
     * @return the time-weighted variance, or 0.0 if total duration is 0
     */
    public double getVariance() {
        if (totalDuration <= 0.0) {
            return 0.0;
        }
        return sumOfSquares / totalDuration;
    }

    /**
     * Gets the running time-weighted standard deviation.
     *
     * @return the time-weighted standard deviation
     */
    public double getStandardDeviation() {
        return Math.sqrt(getVariance());
    }

    /**
     * Gets the total duration accumulated.
     *
     * @return the total duration
     */
    public double getTotalDuration() {
        return totalDuration;
    }

    /**
     * Resets the accumulator to its initial state.
     */
    public void reset() {
        totalDuration = 0.0;
        mean = 0.0;
        sumOfSquares = 0.0;
        lastTime = 0.0;
        currentValue = 0.0;
        initialized = false;
    }
}
