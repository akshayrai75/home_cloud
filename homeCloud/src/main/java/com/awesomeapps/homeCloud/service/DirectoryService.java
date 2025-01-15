package com.awesomeapps.homeCloud.service;

import com.awesomeapps.homeCloud.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class DirectoryService {
    @Value("${homeCloud.root.folder}")
    private String rootFolder;

    /**
     * Create a directory with name <b>dirName</b> at location <b>storagePath</b>.
     */
    public ResponseEntity<String> createDir(@NotNull String dirName, String storagePath) {
        if (dirName.isEmpty()) return ResponseEntity.badRequest().body("Folder name cannot be empty.");
        File newDirectory = new File(Utils.getDirPath(rootFolder, storagePath) + dirName);
        boolean isDirCreated = newDirectory.mkdir();
        if (isDirCreated) {
            return ResponseEntity.ok("Directory created successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to create directory. Please check the name. " +
                    "It may already exist at: " + (storagePath.isEmpty() ? "./" : storagePath));
        }
    }

    /**
     * Rename a directory with name <b>oldDirName</b> at location <b>storagePath</b> to <b>newDirName</b>.
     */
    public ResponseEntity<String> renameDir(String newDirName, String oldDirName, String storagePath) {
        if (new File(Utils.getDirPath(rootFolder, storagePath) + newDirName).exists())
            return ResponseEntity.badRequest().body("Error: Directory with the name" +
                    newDirName + "already exists.");

        File dir = new File(Utils.getDirPath(rootFolder, storagePath) + oldDirName);

        if (!dir.isDirectory()) return ResponseEntity.badRequest().body("Error: Not a directory.");
        else {
            File newDir = new File(dir.getParent() + File.separator + newDirName);
            boolean renameSuccess = dir.renameTo(newDir);
            if (!renameSuccess) return ResponseEntity.badRequest().body("Renaming Failed. Please check the name.");
            else return ResponseEntity.ok("Folder rename successful from " + oldDirName + " to " + newDirName);
        }
    }

    /**
     * Delete a directory with name <b>dirName</b> at location <b>storagePath</b> and move contents to <b>Trash</b>.
     */
    public ResponseEntity<String> deleteDir(@NotNull String storagePath) {
        if (storagePath.isEmpty()) return ResponseEntity.badRequest().body("Folder name cannot be empty.");
        Path dir = Paths.get(Utils.getDirPath(rootFolder, storagePath));
        AtomicReference<String> errorMsg = new AtomicReference<>("");
        try {
            Files.walk(dir).forEach(path -> {
                boolean deleted = Utils.moveToTrash(path);
                if (!deleted) errorMsg.set("Could not delete file: " + path.getFileName().toString());
            });
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        if (!errorMsg.toString().isEmpty()) return ResponseEntity.badRequest().body(errorMsg.toString());
        return ResponseEntity.ok("All Files in the folder moved to trash successfully.");
    }

    /**
     * Copy a directory with name <b>dirName</b> to <b>destinationDirName</b>.
     */
    public ResponseEntity<String> copyDirectory(@NotNull String dirName, String destinationDirName, String storagePath) {
        if (dirName.isEmpty()) return ResponseEntity.badRequest().body("Cannot copy root folder.");

        AtomicReference<String> errorMsg;
        errorMsg = new AtomicReference<>("");

        Path sourceDirPath = Paths.get(Utils.getDirPath(rootFolder, storagePath) + dirName);
        Path destinationDirPath = Paths.get(Utils.getDirPath(rootFolder, destinationDirName) + dirName);

        try {
            Files.createDirectories(destinationDirPath);

            Files.walk(sourceDirPath)
                    .forEach(source -> {
                        Path destination = destinationDirPath.resolve(sourceDirPath.relativize(source));
                        try {
                            Files.copy(source, destination);
                        } catch (IOException e) {
                            errorMsg.set("Could not copy file: " + source.getFileName().toString());
                        }
                    });
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Internal server error copying: " + e.getMessage());
        }

        return ResponseEntity.ok("Folder copied successfully.");
    }

    /**
     * Move a directory with name <b>sourceDirName</b> to <b>destinationDirName</b>.
     */
    public ResponseEntity<String> moveDirectory(@NotNull String sourceDirName, String destinationDirName) {
        if (sourceDirName.isEmpty()) return ResponseEntity.badRequest().body("Cannot move root folder.");
        Path sourceDirPath = Paths.get(Utils.getDirPath(rootFolder, sourceDirName));
        Path targetDirPath = Paths.get(Utils.getDirPath(rootFolder, destinationDirName) + sourceDirName);

        try {
            Files.move(sourceDirPath, targetDirPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (FileAlreadyExistsException ex) {
            ResponseEntity.badRequest().body("File/Folder with same name already exists at destination: " +
                    destinationDirName);
        } catch (IOException ex) {
            ResponseEntity.badRequest().body("Cannot move root folder.");
        }
        return ResponseEntity.ok("Folder moved successfully.");
    }
}
