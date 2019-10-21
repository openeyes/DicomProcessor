package com.abehrdigital.payloadprocessor.utils;


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
        String poolSize = EnvironmentVariableUtils.getEnvironmentVariableReturnNullIfDoesntExist("POOL_SIZE");
        String host = EnvironmentVariableUtils.getEnvironmentVariableReturnNullIfDoesntExist("DATABASE_HOST");
        String port = EnvironmentVariableUtils.getEnvironmentVariableReturnNullIfDoesntExist("DATABASE_PORT");
        String databaseName = EnvironmentVariableUtils.getEnvironmentVariableReturnNullIfDoesntExist("DATABASE_NAME");

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
            username = EnvironmentVariableUtils.getEnvironmentVariableReturnNullIfDoesntExist("DATABASE_USER");
        }
        parameters.put("hibernate.hikari.dataSource.user", username);

        String password;
        try {
            password = Files.asCharSource(new File("run/secrets/DATABASE_PASSWORD"), Charsets.UTF_8).read().trim();
        } catch (Exception e) {
            password = EnvironmentVariableUtils.getEnvironmentVariableReturnNullIfDoesntExist("DATABASE_PASS");
        }
        parameters.put("hibernate.hikari.dataSource.password", password);
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
