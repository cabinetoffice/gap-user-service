package gov.cabinetofice.gapuserservice.service;

import gov.cabinetofice.gapuserservice.config.SpotlightConfig;
import gov.cabinetofice.gapuserservice.model.SpotlightOAuthAudit;
import gov.cabinetofice.gapuserservice.repository.SpotlightOAuthAuditRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotlightServiceTest {

    private SpotlightService spotlightService;

    @Mock
    private SpotlightOAuthAuditRepository spotlightOAuthAuditRepository;

    @Mock
    private SecureRandom secureRandom;;

    private SpotlightConfig spotlightConfig;

    @BeforeEach
    void setUp() {
        spotlightConfig = SpotlightConfig.builder()
                .spotlightUrl("https://test.spotlight.cabinetoffice.gov.uk")
                .clientId("clientId")
                .clientSecret("clientSecret")
                .redirectUri("http://localhost:8080/spotlight/callback")
                .secretName("secretName")
                .build();

        spotlightService = Mockito.spy(new SpotlightService(spotlightConfig, spotlightOAuthAuditRepository, secureRandom));
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void saveAudit_happy_path() {
        when(spotlightOAuthAuditRepository.save(any(SpotlightOAuthAudit.class))).thenReturn(new SpotlightOAuthAudit());
        SpotlightOAuthAudit spotlightOAuthAudit = new SpotlightOAuthAudit();
        spotlightOAuthAudit.setId(1);
        spotlightOAuthAudit.setTimestamp(new Date());
        spotlightService.saveAudit(spotlightOAuthAudit);

        verify(spotlightOAuthAuditRepository, times(1)).save(spotlightOAuthAudit);
    }

    @Test
    void getAuthorizeUrl_happy_path() throws Exception {
        final String expectedResponseType = "code";
        final String expectedScope = "refresh_token api";
        final String expectedState = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        final String expectedCodeVerifier = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        final String expectedCodeChallenge = "HrEA9_Q56Mp8qZRAxQvZE92KA5muvx9LCKm-CieKE1k";
        String authorizationEndpoint = UriComponentsBuilder
                .fromUriString(spotlightConfig.getSpotlightUrl())
                .path("/services")
                .path("/oauth2")
                .path("/authorize")
                .build()
                .toUriString();

        String authUrl = String.format("%s?response_type=%s&client_id=%s&redirect_uri=%s&scope=%s&state=%s&code_challenge=%s&code_challenge_method=S256",
                authorizationEndpoint, expectedResponseType, spotlightConfig.getClientId(), spotlightConfig.getRedirectUri(), expectedScope, expectedState, expectedCodeChallenge);

        byte[] fixedBytes = new byte[2];
        fixedBytes[0] = (byte) 0x00;
        fixedBytes[1] = (byte) 0x00;
        Mockito.doAnswer(invocation -> {
            byte[] bytes = invocation.getArgument(0);
            System.arraycopy(fixedBytes, 0, bytes, 0, fixedBytes.length);
            return null;
        }).when(secureRandom).nextBytes(any(byte[].class));

        String result = spotlightService.getAuthorizeUrl();
        verify(spotlightService, times(1)).getAuthorizeUrl();
        assertThat(result).isEqualTo(authUrl);
    }

    @Test
    void exchangeAuthorizationToken_happy_path() throws IOException {
        final String authorizationToken = "test-token";
        final String state = "test-state";
        ReflectionTestUtils.setField(spotlightService, "state", state);

        spotlightService.exchangeAuthorizationToken(authorizationToken, state);
    }
}