package gov.cabinetofice.gapuserservice.service;

import gov.cabinetofice.gapuserservice.config.SpotlightConfig;
import gov.cabinetofice.gapuserservice.enums.SpotlightOAuthAuditEvent;
import gov.cabinetofice.gapuserservice.enums.SpotlightOAuthAuditStatus;
import gov.cabinetofice.gapuserservice.exceptions.InvalidRequestException;
import gov.cabinetofice.gapuserservice.exceptions.SpotlightInvalidStateException;
import gov.cabinetofice.gapuserservice.model.SpotlightOAuthAudit;
import gov.cabinetofice.gapuserservice.repository.SpotlightOAuthAuditRepository;
import gov.cabinetofice.gapuserservice.util.RestUtils;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.UpdateSecretRequest;

import java.io.IOException;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotlightServiceTest {

    @InjectMocks
    private SpotlightService spotlightService;
    @Mock
    private SpotlightConfig spotlightConfig;

    @Mock
    private SpotlightOAuthAuditRepository spotlightOAuthAuditRepository;

    @Mock
    private SecretsManagerClient secretsManagerClient;

    @Mock
    private GetSecretValueResponse getSecretValueResponse;

    @Mock
    private SecureRandom secureRandom;

    private static MockedStatic<HttpClients> httpClientsMockedStatic;
    private static MockedStatic<RestUtils> restUtilsMockedStatic;

    @Captor
    private ArgumentCaptor<UpdateSecretRequest> argumentCaptor;

    @BeforeEach
    public void before() {
        httpClientsMockedStatic = mockStatic(HttpClients.class);
        restUtilsMockedStatic = mockStatic(RestUtils.class);

        MockitoAnnotations.openMocks(this);
        spotlightConfig = SpotlightConfig.builder()
                .spotlightUrl("https://spotlight.com")
                .clientId("clientId")
                .clientSecret("clientSecret")
                .redirectUri("redirectUrl")
                .build();
        spotlightService = new SpotlightService(spotlightConfig, spotlightOAuthAuditRepository, secureRandom, secretsManagerClient);
        spotlightService.setState("stateValue");
    }

    @AfterEach
    public void close() {
        httpClientsMockedStatic.close();
        restUtilsMockedStatic.close();
    }

    @Test
    void getAuthorizeUrlTest() throws Exception {

        String result = spotlightService.getAuthorizeUrl();

        String expectedUrlPattern = "^https://spotlight\\.com/services/oauth2/authorize\\?" +
                "response_type=code&client_id=clientId&redirect_uri=redirectUrl&scope=refresh_token\\sapi&" +
                "state=[A-Za-z0-9_\\-]+&code_challenge=[A-Za-z0-9_\\-]+&code_challenge_method=S256$";


        assertTrue(result.matches(expectedUrlPattern));
    }

    @Test
    void shouldExchangeAuthorizationTokenTest() throws Exception {

        String secretJson = "{\"secret_string\":\"1234\"}";
        String expectedAccessTokenSecret = "{\"secret_string\":\"1234\",\"access_token\":\"1234\"}";
        String expectedRefreshTokenSecret = "{\"secret_string\":\"1234\",\"refresh_token\":\"5678\"}";
        String expectedResponse = "{\"access_token\":\"1234\", \"refresh_token\":\"5678\"}";

        when(RestUtils.postRequestWithBody(anyString(), anyString(), anyString()))
                .thenReturn(new JSONObject(expectedResponse));

        when(secretsManagerClient.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(getSecretValueResponse);
        when(getSecretValueResponse.secretString()).thenReturn(secretJson);


        spotlightService.exchangeAuthorizationToken("1234", "stateValue");

        verify(secretsManagerClient, times(2)).updateSecret(argumentCaptor.capture());

        assertEquals(expectedAccessTokenSecret, argumentCaptor.getAllValues().get(0).secretString());
        assertEquals(expectedRefreshTokenSecret, argumentCaptor.getAllValues().get(1).secretString());

    }

    @Test
    void shouldThrowInvalidRequestExceptionWhenIOExceptionIsThrown() throws Exception {

        String secretJson = "{\"secret_string\":\"1234\"}";

        when(RestUtils.postRequestWithBody(anyString(), anyString(), anyString()))
                .thenThrow(new IOException());

        when(secretsManagerClient.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(getSecretValueResponse);
        when(getSecretValueResponse.secretString()).thenReturn(secretJson);


        assertThrows(InvalidRequestException.class,
                () -> spotlightService.exchangeAuthorizationToken("1234", "stateValue"));
    }

    @Test
    void shouldThrowExceptionWhenSateValueDoesNotMatchTest() {
        assertThrows(SpotlightInvalidStateException.class,
                () -> spotlightService.exchangeAuthorizationToken("1234", "stateValueInvalid"));

    }

    @Test
    void shouldGetLatestAudit() {
        SpotlightOAuthAudit spotlightOAuthAudit = SpotlightOAuthAudit.builder().id(1).status(
                SpotlightOAuthAuditStatus.FAILURE).event(SpotlightOAuthAuditEvent.AUTHORISE).build();
        when(spotlightOAuthAuditRepository.findFirstByOrderByIdDesc()).thenReturn(spotlightOAuthAudit);

        assertEquals(spotlightOAuthAudit, spotlightService.getLatestAudit());
    }


    @Test
    void shouldRefreshTokenTest() throws Exception {

        String secretJson = "{\"refresh_token\":\"5678\"}";
        String expectedAccessTokenSecret = "{\"refresh_token\":\"5678\",\"access_token\":\"1234\"}";
        String expectedResponse = "{\"access_token\":\"1234\"}";

        when(RestUtils.postRequestWithBody(anyString(), anyString(), anyString()))
                .thenReturn(new JSONObject(expectedResponse));

        when(secretsManagerClient.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(getSecretValueResponse);
        when(getSecretValueResponse.secretString()).thenReturn(secretJson);


        spotlightService.refreshToken();

        verify(secretsManagerClient, times(1)).updateSecret(argumentCaptor.capture());

        assertEquals(expectedAccessTokenSecret, argumentCaptor.getAllValues().get(0).secretString());
    }

    @Test
    void shouldThrowInvalidRequestExceptionWhenIOExceptionIsThrownForRefresh() throws Exception {

        String secretJson = "{\"refresh_token\":\"5678\"}";

        when(RestUtils.postRequestWithBody(anyString(), anyString(), anyString()))
                .thenThrow(new IOException());

        when(secretsManagerClient.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(getSecretValueResponse);
        when(getSecretValueResponse.secretString()).thenReturn(secretJson);


        assertThrows(InvalidRequestException.class,
                () -> spotlightService.refreshToken());
    }
}

