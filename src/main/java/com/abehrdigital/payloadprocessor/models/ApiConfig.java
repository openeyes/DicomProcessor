package com.abehrdigital.payloadprocessor.models;

public class ApiConfig {
    private String host;
    private String port;
    private String username;
    private String password;

    public ApiConfig(String host, String port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
