package gov.cabinetofice.gapuserservice.service;

import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import gov.cabinetofice.gapuserservice.config.SpotlightConfig;
import gov.cabinetofice.gapuserservice.exceptions.InvalidRequestException;
import gov.cabinetofice.gapuserservice.exceptions.SpotlightInvalidStateException;
import gov.cabinetofice.gapuserservice.model.SpotlightOAuthAudit;
import gov.cabinetofice.gapuserservice.repository.SpotlightOAuthAuditRepository;
import gov.cabinetofice.gapuserservice.util.RestUtils;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.UpdateSecretRequest;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@RequiredArgsConstructor
@Service
@Slf4j
@Setter
public class SpotlightService {

    private final SpotlightConfig spotlightConfig;
    private final SpotlightOAuthAuditRepository spotlightOAuthAuditRepository;
    private final SecureRandom secureRandom;

    private final SecretsManagerClient secretsManagerClient;

    // TODO: This won't work in a horizontally scaled environment, as the state will be different for each instance
    private String state;
    private String codeVerifier;
    private String codeChallenge;

    public void saveAudit(SpotlightOAuthAudit spotlightOAuthAudit) {
        spotlightOAuthAuditRepository.save(spotlightOAuthAudit);
    }

    public String getAuthorizeUrl() throws Exception {
        String responseType = "code";
        String scope = "refresh_token api";

        URI uri = UriComponentsBuilder
                .fromUriString(spotlightConfig.getSpotlightUrl())
                .path("/services")
                .path("/oauth2")
                .path("/authorize")
                .build()
                .toUri();

        String authorizationEndpoint = uri.toString();

        state = generateRandomString(64);
        codeVerifier = generateRandomString(128);
        codeChallenge = generateCodeChallenge(codeVerifier);

        log.debug("state: {}", state);
        log.debug("codeVerifier: {}", codeVerifier);
        log.debug("codeChallenge: {}", codeChallenge);

        String authUrl = String.format("%s?response_type=%s&client_id=%s&redirect_uri=%s&scope=%s&state=%s&code_challenge=%s&code_challenge_method=S256",
                authorizationEndpoint, responseType, spotlightConfig.getClientId(), spotlightConfig.getRedirectUri(), scope, state, codeChallenge);

        log.debug("Visit the following URL to authorize spotlight: {}", authUrl);

        return authUrl;
    }

    public void exchangeAuthorizationToken(String authorizationCode, String state) throws IOException {
        String grantType = "authorization_code";

        if (!state.equals(this.state)) {
            throw new SpotlightInvalidStateException("State does not match");
        }

        String tokenEndpoint = UriComponentsBuilder
                .fromUriString(spotlightConfig.getSpotlightUrl())
                .path("/services")
                .path("/oauth2")
                .path("/token")
                .build()
                .toUriString();

        log.debug("SpotlightController callback");
        log.debug("authorizationCode: {}", authorizationCode);
        log.debug("state: {}", state);

        final String requestBody = "code=" + authorizationCode +
                "&client_id=" + spotlightConfig.getClientId() +
                "&client_secret=" + spotlightConfig.getClientSecret() +
                "&redirect_uri=" + spotlightConfig.getRedirectUri() +
                "&code_verifier=" + codeVerifier +
                "&code_challenge=" + codeChallenge +
                "&grant_type=" + grantType;

        JSONObject responseJSON;
        try {
            responseJSON = RestUtils.postRequestWithBody(tokenEndpoint, requestBody,
                    "application/x-www-form-urlencoded");
            String accessToken = responseJSON
                    .get("access_token")
                    .toString();

            String refreshToken = responseJSON
                    .get("refresh_token")
                    .toString();

            log.debug("new Access token: {}", accessToken);
            log.debug("new Refresh token: {}", refreshToken);

            this.updateSecret("access_token", accessToken);
            this.updateSecret("refresh_token", refreshToken);

        } catch (IOException e) {
            this.updateSecret("access_token", null);
            this.updateSecret("refresh_token", null);
            throw new InvalidRequestException("invalid request");
        }
    }

    private String generateRandomString(int length) {
        byte[] bytes = new byte[length];
        this.secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(codeVerifier.getBytes());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private void updateSecret(String name, String value) {
        log.debug("Updating secret {}...", spotlightConfig.getSecretName());

        // Get the current value of the secret
        log.debug("Getting secret {}...", spotlightConfig.getSecretName());
        GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                .secretId(spotlightConfig.getSecretName())
                .build();
        GetSecretValueResponse valueResponse = secretsManagerClient.getSecretValue(valueRequest);
        String secretString = valueResponse.secretString();

        log.debug("Secret: {}", secretString);

        // Parse the secret string into a JSON object
        JsonObject secretJson = JsonParser.parseString(secretString).getAsJsonObject();

        // Update the key-value pairs as needed
        secretJson.addProperty(name, value);

        // Update the secret
        UpdateSecretRequest updateSecretRequest = UpdateSecretRequest.builder()
                .secretId(spotlightConfig.getSecretName())
                .secretString(secretJson.toString())
                .build();
        secretsManagerClient.updateSecret(updateSecretRequest);
    }
}
