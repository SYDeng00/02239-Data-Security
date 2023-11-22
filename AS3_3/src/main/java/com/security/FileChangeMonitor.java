package com.security;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class FileChangeMonitor {
    
    public static void startMonitoring() throws IOException, InterruptedException { //for thread
        ensureLogFileExists();

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            for (Path file : MONITORED_FILES) {
                Path parentDir = file.toAbsolutePath().getParent();
                if (parentDir != null) {
                    parentDir.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
                    System.out.println("Monitoring directory: " + parentDir);
                } else {
                    System.out.println("Parent directory is null for file: " + file);
                }
            }
            // loop for waiting changes
            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path changed = ev.context();

                    for (Path monitoredFile : MONITORED_FILES) {
                        Path resolvedPath = monitoredFile.toAbsolutePath().getParent().resolve(changed);
                        if (resolvedPath.equals(monitoredFile.toAbsolutePath()) && (kind == ENTRY_MODIFY)) {
                            processFileChange(resolvedPath); // invoking processFileChange firstly
                            
                        }
                    }
                }
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        }

}



    
    private static final Path[] MONITORED_FILES = {
        // files should be monitored
        Paths.get("roleHierarchy.properties"),
        Paths.get("userCredentials.properties"),
        Paths.get("userRoles.properties"),
        Paths.get("accessControlPolicy.properties")
    };
    private static final Path LOG_FILE_PATH = Paths.get("LOG_FOR_CHANGE.md");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    
    private static final Map<Path, Long> lastModifiedTimes = new HashMap<>();
    private static final long DEBOUNCE_TIME = 3; // set anti-jitter period

    

    // public static void main(String[] args) throws IOException, InterruptedException {
    //     // make sure the log file exists.
        
    // }
    
    
    //monitoring files for changes
    private static void ensureLogFileExists() throws IOException {
        File logFile = new File(LOG_FILE_PATH.toString());
        if (!logFile.exists()) {
            logFile.createNewFile();
        }
    }
    // function for anti-jitter (avoid modified case be wrote in log many times)
    private static void processFileChange(Path file) throws IOException {
        long lastModifiedTime = Files.getLastModifiedTime(file).toMillis();
        Long lastRecordedTime = lastModifiedTimes.get(file);
        if (lastRecordedTime == null || (lastModifiedTime - lastRecordedTime) > DEBOUNCE_TIME) {
            System.out.println("Processing change for file: " + file);
            lastModifiedTimes.put(file, lastModifiedTime);
            List<String> newContent = Files.readAllLines(file, StandardCharsets.UTF_8);
            appendToChangeLog("Modified: " + file.getFileName(), newContent);
        } else {
            System.out.println("Change skipped due to debounce for file: " + file);
        }
    }
    // record changes into log
    private static void appendToChangeLog(String message, List<String> newContent) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH.toFile(), true))) {
            writer.write("\n### " + dateFormat.format(new Date()) + " - " + message + "\n");
            for (String line : newContent) {
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing to change log: " + e.getMessage());
        }
    }
}
