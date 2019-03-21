package com.abehrdigital.dicomprocessor.utils;

import org.jboss.logging.Logger;

public class EnvironmentVariableUtils {
    public static String getEnvironmentVariableReturnNullIfDoesntExist(String name) {
        String environmentVariable;

        try {
            environmentVariable = System.getenv(name);
        } catch (Exception exception) {
            Logger.getLogger(EnvironmentVariableUtils.class).log(Logger.Level.WARN , exception);
            environmentVariable = null;
        }

        return environmentVariable;
    }
}
