package com.abehrdigital.dicomprocessor.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DirectoryFileNamesReader {

    public static List<String> read(String directoryLocation) throws IOException {
        List<String> filenames = new ArrayList<>();
        File folder = new File(directoryLocation);
        File[] listOfFiles = folder.listFiles();
        if(listOfFiles == null ){
            throw new IOException("Invalid Directory location has been given");
        }
        for (File file : listOfFiles) {
            if (file.isFile()) {
                filenames.add(file.getName());
            }
        }

        return filenames;
    }
}
