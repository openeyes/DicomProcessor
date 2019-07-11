package com.abehrdigital.dicomprocessor.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DirectoryFileNamesReader {

    public static List<String> read(String directoryLocation) throws IOException {
        List<String> filenames = new ArrayList<>();
        File[] listOfFiles = readDirectory(directoryLocation);
        for (File file : listOfFiles) {
            if (file.isFile()) {
                filenames.add(file.getName());
            }
        }

        return filenames;
    }

    public static List<String> read(String directoryLocation, String regex) throws IOException {
        List<String> filenames = new ArrayList<>();
        File[] listOfFiles = readDirectory(directoryLocation);
        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().matches(regex)) {
                filenames.add(file.getName());
            }
        }

        return filenames;
    }

    private static File[] readDirectory(String directoryLocation) throws IOException {

        File folder = new File(directoryLocation);
        File[] listOfFiles = folder.listFiles();
        if(listOfFiles == null ){
            throw new IOException("Invalid Directory location has been given");
        }
        return listOfFiles;
    }
}
