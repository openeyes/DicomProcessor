package com.abehrdigital.payloadprocessor.utils;

import com.abehrdigital.payloadprocessor.RequestWorker;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class JavascriptRoutineMethodConverter {

    public static String convertScriptJavaMethodsWithClassPrefix(String routineScript, Method[] methods, String prefix) {
        String convertedScript = routineScript;
        for (Method method : methods) {
            Logger.getLogger(RequestWorker.class.getName()).log(Level.SEVERE, Pattern.compile("^(?![.])" + method.getName() + "\\(").toString());
            convertedScript = convertedScript.replaceAll(
                    "([?:\\s(])?(?![.])" + method.getName() + "\\(",
                    prefix + "." + method.getName() + "("
            );
        }
        return convertedScript;
    }
}
