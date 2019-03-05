package com.abehrdigital.dicomprocessor.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirectoryFileNamesReader {
    public DirectoryFileNamesReader() {
    }

    public List<String> read(String directoryLocation) {
        List<String> filenames = new ArrayList<>();
        File folder = new File(directoryLocation);
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles) {
            if (file.isFile()) {
                filenames.add(file.getName());
            }
        }

        return filenames;
    }
}
