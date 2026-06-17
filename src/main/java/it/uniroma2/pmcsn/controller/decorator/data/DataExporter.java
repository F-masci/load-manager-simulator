package it.uniroma2.pmcsn.controller.decorator.data;

import java.util.List;
import java.util.Map;

/**
 * Interface for decorators that collect and export simulation data.
 */
public interface DataExporter {
    /**
     * Gets the list of captured data snapshots.
     *
     * @return list of snapshots where each map links metric keys to values
     */
    List<Map<String, Object>> getCapturedData();

    /**
     * Gets the headers representing the data keys.
     *
     * @return array of header names
     */
    String[] getHeaders();
}
