package com.abehrdigital.payloadprocessor.utils;

import com.abehrdigital.payloadprocessor.DicomEngine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RoutineScriptAccessor {


    public RoutineScriptAccessor() {
    }

    public boolean routineExists(String routineName) {
        String trimmedRoutineName = routineName.trim();
        File routineBody = new File(DicomEngine.SCRIPT_FILE_LOCATION+"/" + trimmedRoutineName);
        return routineBody.exists();
    }

    public String getRoutineScript(String routineName) throws IOException {
        String trimmedRoutineName = routineName.trim();
        String routineScript;
        if (routineExists(routineName)) {
            routineScript = new String(Files.readAllBytes(Paths.get(DicomEngine.SCRIPT_FILE_LOCATION + "/" + trimmedRoutineName)));
        } else {
            throw new FileNotFoundException("Routine name : " + trimmedRoutineName + " doesn't exist");
        }
        return routineScript;
    }

    public int getRoutineScriptHashCode(String routineName) throws IOException {
        return getRoutineScript(routineName).hashCode();
    }
}
