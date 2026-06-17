package it.uniroma2.pmcsn.objective;

import it.uniroma2.pmcsn.LoadManagerSimulator;
import it.uniroma2.pmcsn.utils.LogFactory;

/**
 * Base abstract class for all simulation objectives.
 * Provides a common logger and standardized initialization.
 */
public abstract class BaseObjective extends LoadManagerSimulator {
    protected final LogFactory.ModuleLogger logger;

    /**
     * Constructor for the base objective.
     *
     * @param clazz  The objective class for logging purposes.
     * @param module The module name for logging.
     */
    protected BaseObjective(Class<?> clazz, String module) {
        this.logger = LogFactory.getLogger(clazz, module);
    }
}
