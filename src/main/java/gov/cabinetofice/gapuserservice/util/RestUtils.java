package gov.cabinetofice.gapuserservice.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

public class RestUtils {

    //private constructor to hide implicit public one
    private RestUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static JSONObject postRequestWithBody(String url, String body, String contentType) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, contentType);

            httpPost.setEntity(new StringEntity(body));
            HttpResponse response = httpClient.execute(httpPost);
            return convertResponseToJson(response);
        }
    }

    public static JSONObject getRequestWithHeaders(String url, Map<String, String> headers) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            headers.forEach(httpGet::setHeader);

            HttpResponse response = httpClient.execute(httpGet);
            return convertResponseToJson(response);
        }
    }

    public static HttpResponse getRequest(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);

            return httpClient.execute(httpGet);
        }
    }

    private static JSONObject convertResponseToJson(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();

        String responseBody = EntityUtils.toString(entity);
        return new JSONObject(responseBody);
    }
}
