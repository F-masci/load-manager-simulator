package it.uniroma2.pmcsn.controller.decorator.data;

import java.util.List;
import java.util.Map;

/**
 * Interface for decorators that collect simulation data and can export it.
 */
public interface DataExporter {
    /**
     * Returns the list of captured snapshots. Each snapshot is a map of key-value pairs.
     */
    List<Map<String, Object>> getCapturedData();

    /**
     * Returns the ordered list of headers/keys for the data.
     */
    String[] getHeaders();
}
