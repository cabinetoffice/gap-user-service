package gov.cabinetofice.gapuserservice.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

public class RestUtils {

    public static JSONObject postRequestWithBody(String url, String body, String contentType) throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, contentType);

        httpPost.setEntity(new StringEntity(body));
        HttpResponse response = httpClient.execute(httpPost);
        return convertResponseToJson(response);
    }

    public static JSONObject getRequestWithHeaders(String url, Map<String, String> headers) throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        headers.forEach(httpGet::setHeader);

        HttpResponse response = httpClient.execute(httpGet);
        return convertResponseToJson(response);
    }

    public static JSONObject patchRequestWithBody(String url, String body, String contentType) throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPatch httpPatch = new HttpPatch(url);
        httpPatch.setHeader(HttpHeaders.CONTENT_TYPE, contentType);

        httpPatch.setEntity(new StringEntity(body));
        HttpResponse response = httpClient.execute(httpPatch);
        return convertResponseToJson(response);
    }

    private static JSONObject convertResponseToJson(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();

        String responseBody = EntityUtils.toString(entity);
        return new JSONObject(responseBody);
    }

}
