package com.abehrdigital.dicomprocessor.utils;

import com.sun.javaws.exceptions.InvalidArgumentException;
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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PatientSearchApi {
    private static String host = "localhost";
    private static Integer port = 8888;
    private static String authUserName = "admin";
    private static String authUserPassword = "admin";
    private static final String EMPTY_JSON_RESPONSE = "[]";

    public static void init(String configFile) {
        File APIConfig = new File(configFile);
        if (APIConfig.exists() && !APIConfig.isDirectory()) {
            try {
                Wini ini = new Wini(APIConfig);
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
    public static String searchPatient(String hospitalNumber) throws Exception {
        String jsonPatientData = read(hospitalNumber);
        if(jsonPatientData.equals(EMPTY_JSON_RESPONSE)){
            throw new Exception("Empty JSON RESPONSE");
        }
        return getPatientIdFromJson(jsonPatientData);
    }

    private static String getPatientIdFromJson(String jsonPatientData) throws ParseException, InvalidArgumentException {
        int ONE_PATIENT_RESULT = 1;
        JSONParser parser = new JSONParser();
        Object parsedJson = parser.parse(jsonPatientData);
        JSONArray jsonArray= (JSONArray) parsedJson;
        if(jsonArray.size() == ONE_PATIENT_RESULT){
            Iterator jsonArrayIterator = jsonArray.iterator();
            while(jsonArrayIterator.hasNext()){
                JSONObject jsonObject = (JSONObject)jsonArrayIterator.next();
                return (String) jsonObject.get("id");
            }
        } else {
            throw new InvalidArgumentException(new String[]{"More than one patient returned " + jsonPatientData});
        }

        return null;
    }

    /**
     * Trigger a WS call through HTTP for patient search
     * @return The json string of patient data
     * @throws ConnectException
     */
    public static String read(String term)
            throws ConnectException, AuthenticationException, UnsupportedEncodingException {

        String result = "";
        String strURL = "http://" + host + "/api/v1/patient/search?term=" + URLEncoder.encode(term , java.nio.charset.StandardCharsets.UTF_8.toString());

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
