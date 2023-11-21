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
import java.util.List;

public class FileChangeMonitor {
    private static final Path[] MONITORED_FILES = {
        Paths.get("roleHierarchy.properties"),
        Paths.get("userCredentials.properties"),
        Paths.get("userRoles.properties"),
        Paths.get("accessControlPolicy.properties")
    };
    private static final Path LOG_FILE_PATH = Paths.get("LOG_FOR_CHANGE.md");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws IOException, InterruptedException {
        // 确保日志文件存在
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
                            System.out.println("File modified: " + resolvedPath);
                            List<String> newContent = Files.readAllLines(resolvedPath, StandardCharsets.UTF_8);
                            appendToChangeLog("Modified: " + resolvedPath.getFileName(), newContent);
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

    private static void ensureLogFileExists() throws IOException {
        File logFile = new File(LOG_FILE_PATH.toString());
        if (!logFile.exists()) {
            logFile.createNewFile();
        }
    }

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
