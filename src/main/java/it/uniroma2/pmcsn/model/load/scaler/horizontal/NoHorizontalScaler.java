package it.uniroma2.pmcsn.model.load.scaler.horizontal;

import it.uniroma2.pmcsn.model.server.WebServerCluster;

/**
 * A null-object implementation of HorizontalScaler that does nothing and never scales.
 */
public class NoHorizontalScaler extends HorizontalScaler {
    
    public NoHorizontalScaler() {
        super(0.0, 0.0, 0.0);
    }

    @Override
    public void recordCompletion(double clock, double responseTime) {
        // Do nothing
    }

    @Override
    public boolean evaluateScaling(double clock, WebServerCluster cluster) {
        return false;
    }
}
