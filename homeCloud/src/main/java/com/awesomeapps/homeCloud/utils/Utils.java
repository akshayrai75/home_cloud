package com.awesomeapps.homeCloud.utils;

import com.sun.jna.platform.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class Utils {
    public static String getDirPath (String root, String storagePath) {
        return root + File.separator + (storagePath.isEmpty() ? storagePath : storagePath + File.separator);
    }
    public static boolean moveToTrash(Path filePath) {
        File file = filePath.toFile();

        FileUtils fileUtils =  FileUtils.getInstance();
        if (fileUtils.hasTrash()) {
            try {
                fileUtils.moveToTrash(file);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }
}
