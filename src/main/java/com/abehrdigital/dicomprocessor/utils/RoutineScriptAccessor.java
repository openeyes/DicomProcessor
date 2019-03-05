package com.abehrdigital.dicomprocessor.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RoutineScriptAccessor {
    public final String ROUTINE_LIBRARY_LOCATION = "src/main/resources/routineLibrary/";

    public RoutineScriptAccessor() {
    }

    public boolean routineExists(String routineName) {
        File routineBody = new File(ROUTINE_LIBRARY_LOCATION + routineName);
        return routineBody.exists();
    }

    public String getRoutineScript(String routineName) throws IOException {
        String routineScript;
        if (routineExists(routineName)) {
            routineScript = new String(Files.readAllBytes(Paths.get(ROUTINE_LIBRARY_LOCATION + routineName)));
        } else {
            throw new FileNotFoundException("Routine name : " + routineName + " doesn't exist");
        }
        return routineScript;
    }

    public int getRoutineScriptHashCode(String routineName) throws IOException {
        return getRoutineScript(routineName).hashCode();
    }
}
