package com.abehrdigital.dicomprocessor.utils;

import java.lang.reflect.Method;

public class JavascriptRoutineMethodConverter {

    public static String convertScriptJavaMethodsWithClassPrefix(String routineScript, Method[] methods, String prefix) {
        for (Method method : methods) {
            routineScript = routineScript.replace(method.getName() + "(", prefix + "." + method.getName() + "(");
        }
        return routineScript;
    }
}
