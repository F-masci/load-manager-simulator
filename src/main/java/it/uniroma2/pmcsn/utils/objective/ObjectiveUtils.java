package it.uniroma2.pmcsn.utils.objective;

import it.uniroma2.pmcsn.utils.LogFactory;
import it.uniroma2.pmcsn.utils.LogFactory.ModuleLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for objective-related tasks such as CSV export.
 */
public class ObjectiveUtils {
    private static final ModuleLogger logger = LogFactory.getLogger(ObjectiveUtils.class, "OBJ_UTIL");
    private static final String DATA_DIR = "data/objective";
    private static final DateTimeFormatter LOCKED_FILE_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Saves a string to a file in the objective data directory.
     *
     * @param filename the name of the file
     * @param content the content to save
     */
    public static void saveToCsv(String filename, String content) {
        Path filePath = Paths.get(DATA_DIR, filename);
        try {
            writeCsv(filePath, content);
            logger.info("Data saved to {}", filePath);
        } catch (IOException e) {
            Path fallbackPath = lockedFallbackPath(filePath);
            try {
                writeCsv(fallbackPath, content);
                logger.warn("Could not overwrite {} because it is probably open; data saved to {}", filePath, fallbackPath);
            } catch (IOException fallbackError) {
                logger.error("Failed to save CSV data to {}", filename, fallbackError);
            }
        }
    }

    private static void writeCsv(Path filePath, String content) throws IOException {
        Path parent = filePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(content);
        }
    }

    private static Path lockedFallbackPath(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex >= 0 ? fileName.substring(0, extensionIndex) : fileName;
        String extension = extensionIndex >= 0 ? fileName.substring(extensionIndex) : ".csv";
        String suffix = LocalDateTime.now().format(LOCKED_FILE_SUFFIX);
        Path parent = filePath.getParent();
        Path fallbackName = Paths.get(baseName + "_locked_" + suffix + extension);
        return parent == null ? fallbackName : parent.resolve(fallbackName);
    }
}
