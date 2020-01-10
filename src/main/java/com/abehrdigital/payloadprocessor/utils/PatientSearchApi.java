package com.abehrdigital.payloadprocessor.utils;

import com.abehrdigital.payloadprocessor.models.ApiConfig;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;


public class PatientSearchApi {
    private static String host;
    private static String port;
    private static String authUserName;
    private static String authUserPassword;
    private static final String EMPTY_JSON_RESPONSE = "[]";

    public static void init(ApiConfig apiConfig) {
        host = apiConfig.getHost();
        port = apiConfig.getPort();
        authUserName = apiConfig.getUsername();
        authUserPassword = apiConfig.getPassword();
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


        if (jsonPatientData.equals(EMPTY_JSON_RESPONSE)) {
            throw new Exception("Empty JSON RESPONSE");
        }
        return getPatientIdFromJson(jsonPatientData);
    }

    private static String getPatientIdFromJson(String jsonPatientData) throws Exception {
        int ONE_PATIENT_RESULT = 1;
        JSONParser parser = new JSONParser();
        Object parsedJson;
        JSONArray jsonArray;

        parsedJson = parser.parse(jsonPatientData);
        jsonArray = (JSONArray) parsedJson;


        if (jsonArray.size() == ONE_PATIENT_RESULT) {
            Iterator jsonArrayIterator = jsonArray.iterator();
            while (jsonArrayIterator.hasNext()) {
                JSONObject jsonObject = (JSONObject) jsonArrayIterator.next();
                return (String) jsonObject.get("id");
            }
        } else {
            throw new Exception("More than one patient returned " + jsonPatientData);
        }


        return null;
    }

    /**
     * Trigger a WS call through HTTP for patient search
     *
     * @return The json string of patient data
     * @throws ConnectException
     */
    public static String read(String term)
            throws Exception {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
        // Install the all-trusting trust manager
        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        String result = "";
        String strURL = "https://" + host + "/api/v1/patient/search?term=" + URLEncoder.encode(term, java.nio.charset.StandardCharsets.UTF_8.toString());

        HttpGet get = new HttpGet(strURL);
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                authUserName, authUserPassword);

        Header header = new BasicScheme(StandardCharsets.US_ASCII).authenticate(credentials, get, null);
        get.addHeader(header);

        try {
            get.addHeader("Content-type", "text/xml");
            SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                    SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                    NoopHostnameVerifier.INSTANCE);
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(final X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            });
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), NoopHostnameVerifier.INSTANCE);

            CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

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
            throw e;
        } finally {
            get.releaseConnection();
        }

        return result;
    }


}
