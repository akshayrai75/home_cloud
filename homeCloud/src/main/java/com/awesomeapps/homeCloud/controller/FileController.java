package com.awesomeapps.homeCloud.controller;

// Importing required classes

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;

@RestController
public class FileController {

    // Uploading a file
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public String uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("storagePath") String storagePath){

        // Setting up the path of the file
//        String filePath = System.getProperty("user.dir") + File.separator + file.getOriginalFilename();
        String filePath = storagePath + File.separator + file.getOriginalFilename();
        //TODO: Remove below line
        System.out.println(filePath);
        //check file if it is available, if same name with same metadata then skip else add a number to name and continue
        String fileUploadStatus;

        // Try block to check exceptions
        try {

            // Creating an object of FileOutputStream class
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            fileOutputStream.write(file.getBytes());

            // Closing the connection
            fileOutputStream.close();
            fileUploadStatus = "Upload Successful";

        }

        // Catch block to handle exceptions
        catch (Exception e) {
            e.printStackTrace();
            fileUploadStatus = "Error in uploading file: " + e;
        }
        return fileUploadStatus;
    }

    // Getting list of filenames that have been uploaded
    @RequestMapping(value = "/getFiles", method = RequestMethod.GET)
    public String[] getFiles()
    {
        String folderPath = System.getProperty("user.dir") +"/Uploads";

        // Creating a new File instance
        File directory= new File(folderPath);

        // list() method returns an array of strings
        // naming the files and directories
        // in the directory denoted by this abstract pathname

        // returning the list of filenames
        return directory.list();

    }

    // Downloading a file
    @RequestMapping(value = "/download/{path:.+}", method = RequestMethod.GET)
    public ResponseEntity downloadFile(@PathVariable("path") String filename) throws FileNotFoundException {

        // Checking whether the file requested for download exists or not
        String fileUploadPath = System.getProperty("user.dir") +"/Uploads";
        String[] filenames = this.getFiles();
        boolean contains = Arrays.asList(filenames).contains(filename);
        if(!contains) {
            return new ResponseEntity("FIle Not Found",HttpStatus.NOT_FOUND);
        }

        // Setting up the filepath
        String filePath = fileUploadPath+File.separator+filename;

        // Creating new file instance
        File file= new File(filePath);

        // Creating a new InputStreamResource object
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        // Creating a new instance of HttpHeaders Object
        HttpHeaders headers = new HttpHeaders();

        // Setting up values for contentType and headerValue
        String contentType = "application/octet-stream";
        String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                .body(resource);

    }
}

