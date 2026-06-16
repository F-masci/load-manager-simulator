package it.uniroma2.pmcsn.utils.objective;

import it.uniroma2.pmcsn.utils.LogFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for objective-related tasks such as CSV export.
 */
public class ObjectiveUtils {
    private static final LogFactory.ModuleLogger logger = LogFactory.getLogger(ObjectiveUtils.class, "OBJ_UTIL");
    private static final String DATA_DIR = "data/objective";

    /**
     * Saves a string to a file in the objective data directory.
     */
    public static void saveToCsv(String filename, String content) {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            Path filePath = Paths.get(DATA_DIR, filename);
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                writer.write(content);
            }
            logger.info("Data saved to {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to save CSV data to {}", filename, e);
        }
    }
}
