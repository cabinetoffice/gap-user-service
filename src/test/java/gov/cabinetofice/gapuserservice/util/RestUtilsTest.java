package gov.cabinetofice.gapuserservice.util;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class RestUtilsTest {

    private static MockedStatic<HttpClients> httpClientsMockedStatic;

    @BeforeEach
    void setUp() {
        httpClientsMockedStatic = mockStatic(HttpClients.class);

        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void close() {
        httpClientsMockedStatic.close();
    }

    @Test
    void testGetRequestWithHeaders() throws IOException {
        String url = "https://example.com";
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);

        String expectedResponse = "{\"key\":\"value\"}";
        HttpEntity httpEntity = new StringEntity(expectedResponse, ContentType.TEXT_PLAIN);

        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpClient.execute(Mockito.any(HttpGet.class))).thenReturn(httpResponse);

        JSONObject result = RestUtils.getRequestWithHeaders(url, headers);

        verify(httpClient, times(1)).execute(any(HttpGet.class));
        verify(httpResponse, times(1)).getEntity();
        Assertions.assertEquals(expectedResponse, result.toString());
    }

    @Test
    void testPostRequestWithBody() throws IOException {
        String url = "https://example.com";

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);

        String body = "{\"request\":\"true\"}";
        String expectedResponse = "{\"response\":\"true\"}";
        HttpEntity httpEntity = new StringEntity(expectedResponse, ContentType.TEXT_PLAIN);

        when(HttpClients.createDefault()).thenReturn(httpClient);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpClient.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);

        JSONObject result = RestUtils.postRequestWithBody(url, body, ContentType.TEXT_PLAIN.getMimeType());

        verify(httpClient, times(1)).execute(any(HttpPost.class));
        verify(httpResponse, times(1)).getEntity();
        Assertions.assertEquals(expectedResponse, result.toString());
    }


}
