package com.abehrdigital.dicomprocessor.utils;


import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.hibernate.cfg.Configuration;

import java.io.File;
import java.util.HashMap;

public class DatabaseConfiguration {
    private static HashMap<String, String> parameters = new HashMap<>();
    private static Configuration hibernateConfiguration = new Configuration().configure();


    public static void init() {
        initParameters();
        setParametersToHibernateConfiguration();
    }

    private static void initParameters() {
        String poolSize = getEnvironmentVariableReturnNullIfDoesntExist("POOL_SIZE");
        String host = getEnvironmentVariableReturnNullIfDoesntExist("DATABASE_HOST");
        String port = getEnvironmentVariableReturnNullIfDoesntExist("DATABASE_PORT");
        String databaseName = getEnvironmentVariableReturnNullIfDoesntExist("DATABASE_NAME");
        if (host != null && port != null && databaseName != null) {
            String connectionString = "jdbc:mysql://" + host + ":" + port + "/" + databaseName;
            parameters.put("hibernate.hikari.jdbcUrl", connectionString);
        }

        if (poolSize != null) {
            parameters.put("hibernate.hikari.maximumPoolSize", poolSize);
        }

        String username;
        try {
            username = Files.asCharSource(new File("run/secrets/DATABASE_USER"), Charsets.UTF_8).read().trim();
        } catch (Exception exception) {
            username = getEnvironmentVariableReturnNullIfDoesntExist("DATABASE_USER");
        }
        parameters.put("hibernate.hikari.dataSource.user", username);

        String password;
        try {
            password = Files.asCharSource(new File("run/secrets/DATABASE_PASSWORD"), Charsets.UTF_8).read().trim();
        } catch (Exception e) {
            password = getEnvironmentVariableReturnNullIfDoesntExist("DATABASE_PASS");
        }
        parameters.put("hibernate.hikari.dataSource.password", password);
    }

    private static String getEnvironmentVariableReturnNullIfDoesntExist(String name) {
        String environmentVariable;

        try {
            environmentVariable = System.getenv(name);
        } catch (Exception exception) {
            environmentVariable = null;
        }

        return environmentVariable;
    }

    private static void setParametersToHibernateConfiguration() {
        for (HashMap.Entry<String, String> parameter : parameters.entrySet()) {
            setPropertyIfValueIsNotNull(parameter);
        }
    }

    private static void setPropertyIfValueIsNotNull(HashMap.Entry<String, String> parameter) {
        if (parameter.getValue() != null) {
            hibernateConfiguration.setProperty(parameter.getKey(), parameter.getValue());
        }
    }

    public static Configuration getHibernateConfiguration() {
        return hibernateConfiguration;
    }
}
