package com.awesomeapps.homeCloud.controller;

import com.awesomeapps.homeCloud.models.FileDto;
import com.awesomeapps.homeCloud.service.FileService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class FileController {

    @Autowired
    private FileService fileService;

    /**
     * Upload a <b>file</b> at location <b>storagePath</b>.
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public ResponseEntity<String> uploadFile(@RequestParam("file") @NotNull MultipartFile file,
                                             @RequestParam("storagePath") String storagePath) {

        return fileService.uploadFile(file, storagePath);
    }

    /**
     * Get the list of files at location <b>storagePath</b>.
     */
    @GetMapping("/listFiles")
    public ResponseEntity<List<FileDto>> listFiles(@RequestParam("storagePath") String storagePath)
            throws IOException {
        return fileService.listFiles(storagePath);
    }


    /**
     * Get the list of files at location <b>storagePath</b>.
     */
    @GetMapping("/getFile")
    public ResponseEntity<Resource> getFile(@RequestParam("fileName") @NotNull String fileName,
                                            @RequestParam("storagePath") String storagePath)
            throws IOException {
        return fileService.getFile(fileName, storagePath);
    }

    /**
     * Download a file with <b>filePath</b>.
     */
    @GetMapping("/downloadAFile")
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam("filePath") @NotNull String filePath)
            throws IOException {
        return fileService.downloadFile(filePath);
    }

    /**
     * Download multiple files as a Zip using <b>filePaths</b> of all the required files.
     */
    @GetMapping("/downloadMultipleFiles")
    public ResponseEntity<InputStreamResource> downloadMultipleFiles(
            @RequestParam("filePaths") @NotNull List<String> filePaths) throws IOException {
        return fileService.downloadMultipleFiles(filePaths);
    }

    /**
     * Rename a file with name <b>oldFileName</b> at location <b>storagePath</b> to <b>newFileName</b>.
     */
    @RequestMapping(value = "/rename", method = RequestMethod.POST)
    public ResponseEntity<String> renameDir(@RequestParam("newFileName") @NotNull String newFileName,
                                            @RequestParam("oldFileName") @NotNull String oldFileName,
                                            @RequestParam("storagePath") String storagePath) {
        return fileService.renameFile(newFileName, oldFileName, storagePath);
    }

    /**
     * Delete a file with name <b>fileName</b> at location <b>storagePath</b> and move it to the <b>Trash</b>.
     */
//    @RequestMapping(value = "/delete", method = RequestMethod.POST)
//    public ResponseEntity<String> deleteDir(@RequestParam("fileName") @NotNull String fileName,
//                                            @RequestParam("storagePath") String storagePath)
//    {
//        return fileService.deleteFile(fileName, storagePath);
//    }
    @RequestMapping(value = "/deleteFiles", method = RequestMethod.POST)
    public ResponseEntity<String> deleteFiles(
            @RequestParam("fileName") @NotNull List<String> fileNames,
            @RequestParam("storagePath") String storagePath) {
        return fileService.deleteFiles(fileNames, storagePath);
    }

    /**
     * Copy files with names in <b>fileNames</b> to <b>storagePath</b>.
     */
    @RequestMapping(value = "/copyFiles", method = RequestMethod.POST)
    public ResponseEntity<String> copyDirectory(@RequestParam("fileNames") @NotNull List<String> fileNames,
                                                @RequestParam("destinationDirName") @NotNull String destinationDirName,
                                                @RequestParam("storagePath") String storagePath) {
        return fileService.copyFiles(fileNames, destinationDirName, storagePath);
    }

    /**
     * Move a files with names in  <b>sourceFileNames</b> to <b>destinationDirName</b>.
     */
    @RequestMapping(value = "/moveFiles", method = RequestMethod.POST)
    public ResponseEntity<String> moveDirectory(@RequestParam("sourceFileName") @NotNull List<String> sourceFileNames,
                                                @RequestParam("destinationDirName") String destinationDirName,
                                                @RequestParam("storagePath") String storagePath) {
        return fileService.moveFiles(sourceFileNames, destinationDirName, storagePath);
    }
}

