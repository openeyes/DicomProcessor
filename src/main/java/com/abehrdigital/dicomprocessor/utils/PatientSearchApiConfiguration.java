package com.abehrdigital.dicomprocessor.utils;

import com.abehrdigital.dicomprocessor.models.ApiConfig;

public class PatientSearchApiConfiguration {
    private static String host = "localhost";
    private static String port = "8888";
    private static String username = "admin";
    private static String password = "admin";
    private static ApiConfig apiConfig = null;

    public static void init() {
        String environmentVariableValue;
        environmentVariableValue = EnvironmentVariableUtils.getEnvironmentVariableReturnNullIfDoesntExist("API_HOST");
        if (environmentVariableValue != null) {
            host = environmentVariableValue;
        }
        environmentVariableValue = EnvironmentVariableUtils.getEnvironmentVariableReturnNullIfDoesntExist("API_PORT");
        if (environmentVariableValue != null) {
            port = environmentVariableValue;
        }
        environmentVariableValue = EnvironmentVariableUtils.getEnvironmentVariableReturnNullIfDoesntExist("API_USER");
        if (environmentVariableValue != null) {
            username = environmentVariableValue;
        }
        environmentVariableValue = EnvironmentVariableUtils.getEnvironmentVariableReturnNullIfDoesntExist("API_PASSWORD");
        if (environmentVariableValue != null) {
            password = environmentVariableValue;
        }
        apiConfig = new ApiConfig(host, port, username, password);
    }


    public static ApiConfig getApiConfig() {
        if (apiConfig == null) {
            init();
        }
        return apiConfig;
    }
}
