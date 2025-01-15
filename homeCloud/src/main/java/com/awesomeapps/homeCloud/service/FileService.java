package com.awesomeapps.homeCloud.service;

import com.awesomeapps.homeCloud.models.FileDto;
import com.awesomeapps.homeCloud.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileService {

    @Value("${homeCloud.root.folder}")
    private String rootFolder;

    @Autowired
    private DirectoryService directoryService;

    /**
     * Upload a <b>file</b> at <b>storagePath</b> and if a file with same name exists at the location,
     * add a number to the file name and then add the file at the location.
     */
    public ResponseEntity<String> uploadFile(@NotNull MultipartFile file, String storagePath) {
        // Setting up the path of the file
        Path filePath = Paths.get(rootFolder, storagePath, file.getOriginalFilename());

        try {
            // Check if the file already exists and resolve naming conflicts
            filePath = resolveNamingConflict(filePath);

            // Copy the file to the target location
            Files.copy(file.getInputStream(), filePath);

            return ResponseEntity.ok("Upload Successful");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error in uploading file: " + e.getMessage());
        }
    }

    /**
     * Get a file to view using <b>fileName</b> at <b>storagePath</b>.
     */
    public ResponseEntity<Resource> getFile(String fileName, String storagePath) throws IOException {
        // Construct the base directory path
        String dirPath = Utils.getDirPath(rootFolder, storagePath);

        // Construct the path for the file
        Path filePath = Paths.get(dirPath, fileName);

        if (Files.exists(filePath)) {
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(filePath));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * Get the list of all the files at as a <b>FileDto</b> which contains file information <i>like name, type, size
     * and creationDate</i>, located at <b>storagePath</b>.
     */
    public ResponseEntity<List<FileDto>> listFiles(String storagePath) throws IOException {
        List<FileDto> fileList = new ArrayList<>();
        List<File> filesAndDirs = listFilesInDirectory(rootFolder + File.separator + storagePath);

        for (File file : filesAndDirs) {
            long size = file.length();
            String creationDate = Files.getLastModifiedTime(file.toPath()).toString();

            fileList.add(new FileDto(file.getName(), file.isFile() ? "file" : "directory", size, creationDate));
        }

        return ResponseEntity.ok(fileList);
    }

    /**
     * Download a file with <b>filePath</b>.
     */
    public ResponseEntity<InputStreamResource> downloadFile(String filePath) throws FileNotFoundException {
        // Validate and sanitize the filePath to prevent directory traversal attacks
        Path sanitizedPath = Paths.get(rootFolder).resolve(filePath).normalize();

        // Check if the path is outside the intended directory
        if (!sanitizedPath.startsWith(Paths.get(rootFolder))) {
            return ResponseEntity.badRequest().body(null);
        }

        File file = sanitizedPath.toFile();

        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        FileInputStream fileInputStream = new FileInputStream(file);
        InputStreamResource resource = new InputStreamResource(fileInputStream);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * Download multiple files as a Zip using <b>filePaths</b> of all the required files.
     */
    public ResponseEntity<InputStreamResource> downloadMultipleFiles(@NotNull List<String> filePaths) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        for (String filePath : filePaths) {
            // Validate and sanitize the filePath to prevent directory traversal attacks
            Path sanitizedPath = Paths.get(rootFolder).resolve(filePath).normalize();

            // Check if the path is outside the intended directory
            if (!sanitizedPath.startsWith(Paths.get(rootFolder))) {
                return ResponseEntity.badRequest().body(null);
            }

            File file = sanitizedPath.toFile();

            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }

            // Add file to ZIP
            FileInputStream fis = new FileInputStream(file);
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            zos.closeEntry();
            fis.close();
        }

        zos.close();
        baos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        InputStreamResource resource = new InputStreamResource(bais);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=files.zip");
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(baos.size())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * Rename a file from <b>oldFileName</b> to <b>newFileName</b> at location <b>storagePath</b> if a file
     * with same name does not exist at the same location;
     */
    public ResponseEntity<String> renameFile(String newFileName, String oldFileName, String storagePath) {

        // Construct the directory path
        String dirPath = Utils.getDirPath(rootFolder, storagePath);

        // Setting up the paths of the old and new files
        Path oldFilePath = Paths.get(dirPath, oldFileName);
        Path newFilePath = Paths.get(dirPath, newFileName);

        try {
            // Check if the old file exists
            if (!Files.exists(oldFilePath)) {
                return ResponseEntity.badRequest().body("Error: Old file not found");
            }

            // Check if a file with the new name already exists
            if (Files.exists(newFilePath)) {
                return ResponseEntity.badRequest().body("Error: A file with the new name already exists");
            }

            // Renaming the file
            Files.move(oldFilePath, newFilePath);

            return ResponseEntity.ok("File renamed successfully");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error in renaming file: " + e.getMessage());
        }

    }
/*
    public ResponseEntity<String> deleteFile(String fileName, String storagePath) {
        // Use the getDirPath utility function to construct the directory path
        String dirPath = Utils.getDirPath(rootFolder, storagePath);

        // Setting up the path of the file to be deleted using the directory path
        Path filePath = Paths.get(dirPath, fileName);

        // Move the file to trash using your utility function
        if (Utils.moveToTrash(filePath)) {
            return ResponseEntity.ok("File moved to trash successfully");
        } else {
            return ResponseEntity.badRequest().body("Error in moving file to trash");
        }
    }
    */

    /**
     * Delete file(s) with names in <b>fileNames</b> at location <b>storagePath</b>.
     */
    public ResponseEntity<String> deleteFiles(@NotNull List<String> fileNames, String storagePath) {
        // Construct the directory path
        String dirPath = Utils.getDirPath(rootFolder, storagePath);

        StringBuilder responseMessage = new StringBuilder();
        boolean allFilesDeleted = true;

        for (String fileName : fileNames) {
            // Setting up the path of the file to be moved to trash
            Path filePath = Paths.get(dirPath, fileName);

            // Check if the path is a directory
            if (Files.isDirectory(filePath)) {
                ResponseEntity<String> response = directoryService.deleteDir(
                        storagePath + File.separator + fileName);
                if (response.getStatusCode().is2xxSuccessful()) {
                    responseMessage.append("Directory ")
                            .append(fileName)
                            .append(" moved to trash successfully.\n");
                } else {
                    responseMessage.append(response.getBody())
                            .append("\n");
                    allFilesDeleted = false;
                }
            } else {
                // Move the file to trash
                if (Utils.moveToTrash(filePath)) {
                    responseMessage.append("File ").append(fileName)
                            .append(" moved to trash successfully.\n");
                } else {
                    responseMessage.append("Error in moving file ").append(fileName)
                            .append(" to trash.\n");
                    allFilesDeleted = false;
                }
            }
        }

        if (allFilesDeleted) {
            return ResponseEntity.ok(responseMessage.toString());
        } else {
            return ResponseEntity.badRequest().body(responseMessage.toString());
        }
    }

    /**
     * Copy files in <b>fileNames</b> from <b>storagePath</b> to <b>destinationDirName</b>.
     */
    public ResponseEntity<String> copyFiles(
            @NotNull List<String> fileNames, String destinationDirName, String storagePath) {
        // Construct the base directory paths
        String dirPath = Utils.getDirPath(rootFolder, storagePath);
        String destinationPath = Utils.getDirPath(rootFolder, destinationDirName);

        StringBuilder responseMessage = new StringBuilder();
        boolean allFilesCopied = true;

        for (String fileName : fileNames) {
            // Construct the path for the file or directory
            Path sourcePath = Paths.get(dirPath, fileName);
            Path destinationFilePath = Paths.get(destinationPath, fileName);

            try {
                // Check if the path is a directory
                if (Files.isDirectory(sourcePath)) {
                    // Copy the directory
                    ResponseEntity<String> response = directoryService.copyDirectory(
                            fileName, destinationDirName, storagePath);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        responseMessage.append("Directory ")
                                .append(fileName)
                                .append(" copied successfully.\n");
                    } else {
                        responseMessage.append(response.getBody()).append("\n");
                        allFilesCopied = false;
                    }
                } else {
                    // Copy the file
                    Files.copy(sourcePath, destinationFilePath, StandardCopyOption.REPLACE_EXISTING);
                    responseMessage.append("File ")
                            .append(fileName)
                            .append(" copied successfully.\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
                responseMessage.append("Error in copying ")
                        .append(fileName)
                        .append(": ")
                        .append(e.getMessage())
                        .append("\n");
                allFilesCopied = false;
            }
        }

        if (allFilesCopied) {
            return ResponseEntity.ok(responseMessage.toString());
        } else {
            return ResponseEntity.badRequest().body(responseMessage.toString());
        }
    }

    /**
     * Move files in <b>sourceFileNames</b> from <b>storagePath</b> to <b>destinationDirName</b>.
     */
    public ResponseEntity<String> moveFiles(
            @NotNull List<String> sourceFileNames, String destinationDirName, String storagePath) {
        // Construct the base directory paths
        String sourceDirPath = Utils.getDirPath(rootFolder, storagePath);
        String destinationPath = Utils.getDirPath(rootFolder, destinationDirName);

        StringBuilder responseMessage = new StringBuilder();
        boolean allFilesMoved = true;

        for (String fileName : sourceFileNames) {
            // Construct the path for the file or directory
            Path sourcePath = Paths.get(sourceDirPath, fileName);
            Path destinationFilePath = Paths.get(destinationPath, fileName);

            try {
                // Check if the path is a directory
                if (Files.isDirectory(sourcePath)) {
                    // Use the moveDirectory method from fileService to move the directory
                    ResponseEntity<String> response = directoryService.moveDirectory(fileName, destinationDirName);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        responseMessage.append("Directory ").append(fileName).append(" moved successfully.\n");
                    } else {
                        responseMessage.append(response.getBody()).append("\n");
                        allFilesMoved = false;
                    }
                } else {
                    // Move the file
                    Files.move(sourcePath, destinationFilePath);
                    responseMessage.append("File ").append(fileName).append(" moved successfully.\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
                responseMessage.append("Error in moving ").append(fileName).append(": ").append(e.getMessage()).append("\n");
                allFilesMoved = false;
            }
        }

        if (allFilesMoved) {
            return ResponseEntity.ok(responseMessage.toString());
        } else {
            return ResponseEntity.badRequest().body(responseMessage.toString());
        }

    }

    private Path resolveNamingConflict(@NotNull Path filePath) throws IOException {
        int count = 1;
        String originalFileName = filePath.getFileName().toString();
        String baseName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));

        while (Files.exists(filePath)) {
            filePath = filePath.getParent().resolve(baseName + "(" + count + ")" + extension);
            count++;
        }

        return filePath;
    }

    private @NotNull List<File> listFilesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        List<File> fileList = new ArrayList<>();

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                fileList.addAll(Arrays.asList(files));
            }
        }
        return fileList;
    }

}
