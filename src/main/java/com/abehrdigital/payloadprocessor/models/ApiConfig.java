package com.abehrdigital.payloadprocessor.models;

public class ApiConfig {
    private String host;
    private String port;
    private String username;
    private String password;
    private Boolean doHttps;

    public ApiConfig(String host, String port, String username, String password, Boolean doHttps) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.doHttps = doHttps;
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

    public Boolean getDoHttps() {
        return doHttps;
    }
}
