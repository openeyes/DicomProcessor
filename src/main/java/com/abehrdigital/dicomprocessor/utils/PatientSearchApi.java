package com.abehrdigital.dicomprocessor.utils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PatientSearchApi {
    private static String host = "localhost";
    private static Integer port = 8888;
    private static String authUserName = "admin";
    private static String authUserPassword = "admin";

    public static void init(String configFile) {
        File APIConfig = new File(configFile);
        if (APIConfig.exists() && !APIConfig.isDirectory()) {
            Wini ini = null;
            try {
                ini = new Wini(APIConfig);
                host = ini.get("?", "api_host");
                port = Integer.parseInt(ini.get("?", "api_port"));
                authUserName = ini.get("?", "api_user");
                authUserPassword = ini.get("?", "api_password");
            } catch (Exception ex) {
                Logger.getLogger(PatientSearchApi.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Trigger a search for patient WS call
     *
     * @param hospitalNumber the hospital number of the patient to search for
     * @return The status code of the HTTP response
     * @throws ConnectException
     */
    public static String searchPatient(String hospitalNumber) throws ConnectException, AuthenticationException {
        return read("Patient", "identifier=" + hospitalNumber);
    }

    /**
     * Trigger a WS call through HTTP for patient search
     *
     * @param resourceType  The REST resource name (only "Patient" supported now)
     * @param requestParams The arguments for the HTTP call
     * @return The status code from the HTTP answer
     * @throws ConnectException
     */
    public static String read(String resourceType, String requestParams)
            throws ConnectException, AuthenticationException {

        String result = "";
        String strURL = "http://" + host + ":" + port + "/api/"
                + resourceType + "?resource_type=Patient&_format=xml";

        System.out.println("URL " + strURL);
        if (requestParams != null) {
            strURL += "&" + requestParams;
        }
        HttpGet get = new HttpGet(strURL);
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                authUserName, authUserPassword);

        Header header = new BasicScheme(StandardCharsets.US_ASCII).authenticate(credentials, get, null);
        get.addHeader(header);

        try {
            get.addHeader("Content-type", "text/xml");
            HttpClientBuilder builder = HttpClientBuilder.create();
            CloseableHttpClient httpclient = builder.build();

            CloseableHttpResponse httpResponse = httpclient.execute(get);

            HttpEntity entity2 = httpResponse.getEntity();
            StringWriter writer = new StringWriter();
            //IOUtils.copy(entity2.getContent(), writer);
            result = EntityUtils.toString(entity2);
            EntityUtils.consume(entity2);

        } catch (ConnectException e) {
            // this happens when there's no server to connect to
            e.printStackTrace();
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            get.releaseConnection();
        }
        return result;
    }

}
