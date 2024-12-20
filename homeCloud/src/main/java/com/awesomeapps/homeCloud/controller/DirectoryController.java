package com.awesomeapps.homeCloud.controller;

import com.awesomeapps.homeCloud.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;

@RequestMapping(value = "/dir")
@RestController
public class DirectoryController {
    @Value("${homeCloud.root.folder}")
    private String rootFolder;

    /**
     * Create a directory with name <b>dirName</b> at location <b>storagePath</b>.
     * */
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public ResponseEntity createDir(@RequestParam("dirName") @NotNull String dirName,
                                   @RequestParam("storagePath") String storagePath)
    {
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
     * */
    @RequestMapping(value = "/rename", method = RequestMethod.POST)
    public ResponseEntity renameDir(@RequestParam("newDirName") String newDirName,
                                   @RequestParam("oldDirName") String oldDirName,
                                   @RequestParam("storagePath") String storagePath)
    {
        if (new File(Utils.getDirPath(rootFolder, storagePath) + newDirName).exists())
            return ResponseEntity.badRequest().body("Error: Directory with the name"+
                    newDirName +"already exists.");

        File dir = new File(Utils.getDirPath(rootFolder, storagePath) + oldDirName);

        if (!dir.isDirectory()) return ResponseEntity.badRequest().body("Error: Not a directory.");
        else {
            File newDir = new File(dir.getParent() + File.separator + newDirName);
            boolean renameSuccess  = dir.renameTo(newDir);
            if (!renameSuccess) return ResponseEntity.badRequest().body("Renaming Failed. Please check the name.");
            else return ResponseEntity.ok("Folder rename successful from "+oldDirName+" to "+newDirName);
        }
    }

    /**
     * Delete a directory with name <b>dirName</b> at location <b>storagePath</b> and move contents to <b>Trash</b>.
     * */
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResponseEntity deleteDir(@RequestParam("storagePath") @NotNull String storagePath)
    {
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
        if (!errorMsg.toString().isEmpty()) return ResponseEntity.badRequest().body(errorMsg);
        return ResponseEntity.ok("All Files in the folder moved to trash successfully.");
    }

    /**
     * Copy a directory with name <b>dirName</b> to <b>destinationDirName</b>.
     * */
    @RequestMapping(value = "/copy", method = RequestMethod.POST)
    public ResponseEntity<String> copyDirectory(@RequestParam("dirName") String dirName,
                                                @RequestParam("destinationDirName") String destinationDirName,
                                                @RequestParam("storagePath") String storagePath) {
        if (dirName.isEmpty()) return ResponseEntity.badRequest().body("Cannot copy root folder.");

        AtomicReference<String> errorMsg = new AtomicReference<>("");

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
     * */
    @RequestMapping(value = "/move", method = RequestMethod.POST)
    public ResponseEntity moveDirectory(@RequestParam("sourceDirName") String sourceDirName,
                                        @RequestParam("destinationDirName") String destinationDirName) {
        if (sourceDirName.isEmpty()) return ResponseEntity.badRequest().body("Cannot move root folder.");
        Path sourceDirPath = Paths.get(Utils.getDirPath(rootFolder,sourceDirName));
        Path targetDirPath = Paths.get(Utils.getDirPath(rootFolder,destinationDirName) + sourceDirName);

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
