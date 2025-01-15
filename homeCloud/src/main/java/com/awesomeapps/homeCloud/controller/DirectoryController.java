package com.awesomeapps.homeCloud.controller;

import com.awesomeapps.homeCloud.service.DirectoryService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(value = "/dir")
@RestController
public class DirectoryController {

    @Autowired
    private DirectoryService directoryService;

    /**
     * Create a directory with name <b>dirName</b> at location <b>storagePath</b>.
     */
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public ResponseEntity<String> createDir(@RequestParam("dirName") @NotNull String dirName,
                                            @RequestParam("storagePath") String storagePath) {
        return directoryService.createDir(dirName, storagePath);
    }

    /**
     * Rename a directory with name <b>oldDirName</b> at location <b>storagePath</b> to <b>newDirName</b>.
     */
    @RequestMapping(value = "/rename", method = RequestMethod.POST)
    public ResponseEntity<String> renameDir(@RequestParam("newDirName") String newDirName,
                                            @RequestParam("oldDirName") String oldDirName,
                                            @RequestParam("storagePath") String storagePath) {
        return directoryService.renameDir(newDirName, oldDirName, storagePath);
    }

    /**
     * Delete a directory with name <b>dirName</b> at location <b>storagePath</b> and move contents to <b>Trash</b>.
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResponseEntity<String> deleteDir(@RequestParam("storagePath") @NotNull String storagePath) {
        return directoryService.deleteDir(storagePath);
    }

    /**
     * Copy a directory with name <b>dirName</b> to <b>destinationDirName</b>.
     */
    @RequestMapping(value = "/copy", method = RequestMethod.POST)
    public ResponseEntity<String> copyDirectory(@RequestParam("dirName") String dirName,
                                                @RequestParam("destinationDirName") String destinationDirName,
                                                @RequestParam("storagePath") String storagePath) {
        return directoryService.copyDirectory(dirName, destinationDirName, storagePath);
    }


    /**
     * Move a directory with name <b>sourceDirName</b> to <b>destinationDirName</b>.
     */
    @RequestMapping(value = "/move", method = RequestMethod.POST)
    public ResponseEntity<String> moveDirectory(@RequestParam("sourceDirName") String sourceDirName,
                                                @RequestParam("destinationDirName") String destinationDirName) {
        return directoryService.moveDirectory(sourceDirName, destinationDirName);
    }
}
