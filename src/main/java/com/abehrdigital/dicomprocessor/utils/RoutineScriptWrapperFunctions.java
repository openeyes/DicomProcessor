package com.abehrdigital.dicomprocessor.utils;


import com.abehrdigital.dicomprocessor.DicomEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoutineScriptWrapperFunctions {

    private static final String postScriptLocation = "/wrapperScripts/postScript/save";
    private static final String preScriptLocation = "/wrapperScripts/preScript/functions";

    private static String postScripts = "";
    private static String preScripts = "";

    public static void init() {
        InputStream postScriptInputStream = DicomEngine.class.getResourceAsStream(postScriptLocation);
        InputStream preScriptInputStream = DicomEngine.class.getResourceAsStream(preScriptLocation);
        try {
            preScripts = readFromInputStream(preScriptInputStream);
            postScripts = readFromInputStream(postScriptInputStream);
        } catch (IOException exception) {
            Logger.getLogger(DicomEngine.class.getName()).log(Level.SEVERE,
                    StackTraceUtil.getStackTraceAsString(exception));
        }
    }

    public static String addWrapperScripts(String script) {
        return preScripts + " \n " + script + " \n " + postScripts;
    }

    private static String readFromInputStream(InputStream inputStream)
            throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

}
