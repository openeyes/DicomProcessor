package com.abehrdigital.payloadprocessor.utils;

import java.lang.reflect.Method;


public class JavascriptRoutineMethodConverter {

    public static String convertScriptJavaMethodsWithClassPrefix(String routineScript, Method[] methods, String prefix) {
        String convertedScript = routineScript;
        for (Method method : methods) {
            convertedScript = convertedScript.replaceAll(
                    "(?<![.])" + method.getName() + "\\(",
                    prefix + "." + method.getName() + "("
            );
        }
        return convertedScript;
    }
}
