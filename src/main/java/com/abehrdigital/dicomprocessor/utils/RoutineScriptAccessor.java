package com.abehrdigital.dicomprocessor.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoutineScriptAccessor {
    private final String ROUTINE_LIBRARY_LOCATION = "src/main/resources/routineLibrary/";

    public RoutineScriptAccessor() {
    }

    public boolean routineExists(String routineName) {
        File routineBody = new File(ROUTINE_LIBRARY_LOCATION + routineName);
        return routineBody.exists();
    }

    public String getRoutineScript(String routineName) throws IOException {
        String routineScript = null;
        if (routineExists(routineName)) {
            routineScript = new String(Files.readAllBytes(Paths.get(ROUTINE_LIBRARY_LOCATION + routineName)));
        }
        return routineScript;
    }
}
