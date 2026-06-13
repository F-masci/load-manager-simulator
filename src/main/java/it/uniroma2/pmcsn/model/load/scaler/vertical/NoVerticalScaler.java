package it.uniroma2.pmcsn.model.load.scaler.vertical;

import it.uniroma2.pmcsn.model.server.SpikeServer;

/**
 * A null-object implementation of VerticalScaler that does nothing and never scales.
 */
public class NoVerticalScaler extends VerticalScaler {
    
    public NoVerticalScaler() {
        super(0.0, 0.0, 0.0);
    }

    @Override
    public boolean evaluateScaling(double clock, SpikeServer spikeServer) {
        return false;
    }
}
