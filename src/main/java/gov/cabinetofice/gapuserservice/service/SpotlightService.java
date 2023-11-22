package gov.cabinetofice.gapuserservice.service;

import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import gov.cabinetofice.gapuserservice.config.SpotlightConfig;
import gov.cabinetofice.gapuserservice.exceptions.SpotlightInvalidStateException;
import gov.cabinetofice.gapuserservice.model.SpotlightOAuthAudit;
import gov.cabinetofice.gapuserservice.repository.SpotlightOAuthAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.UpdateSecretRequest;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class SpotlightService {

    private final SpotlightConfig spotlightConfig;
    private final SpotlightOAuthAuditRepository spotlightOAuthAuditRepository;

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

        URI uri = UriComponentsBuilder
                .fromUriString(spotlightConfig.getSpotlightUrl())
                .path("/services")
                .path("/oauth2")
                .path("/token")
                .build()
                .toUri();

        String tokenEndpoint = uri.toString();

        log.debug("SpotlightController callback");
        log.debug("authorizationCode: {}", authorizationCode);
        log.debug("state: {}", state);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(tokenEndpoint);

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("code", authorizationCode));
            params.add(new BasicNameValuePair("client_id", spotlightConfig.getClientId()));
            params.add(new BasicNameValuePair("client_secret", spotlightConfig.getClientSecret()));
            params.add(new BasicNameValuePair("redirect_uri", spotlightConfig.getRedirectUri()));
            params.add(new BasicNameValuePair("code_verifier", codeVerifier));
            params.add(new BasicNameValuePair("code_challenge", codeChallenge));
            params.add(new BasicNameValuePair("grant_type", grantType));
            httpPost.setEntity(new UrlEncodedFormEntity(params));

            String responseString = httpClient.execute(httpPost, httpResponse ->
                    EntityUtils.toString(httpResponse.getEntity()));

            log.debug("Spotlight token exchange response: " + responseString);

            JSONObject responseJSON = new JSONObject(responseString);
            String accessToken = responseJSON
                    .get("access_token")
                    .toString();

            String refreshToken = responseJSON
                    .get("refresh_token")
                    .toString();

            this.updateSecret("access_token", accessToken);
            this.updateSecret("refresh_token", refreshToken);

        } catch (Exception e) {
            this.updateSecret("access_token", null);
            this.updateSecret("refresh_token", null);
            throw e;
        }
    }

    private static String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(codeVerifier.getBytes());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private void updateSecret(String name, String value) {
        log.info("Updating secret {}...", spotlightConfig.getSecretName());
        SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();

            // Get the current value of the secret
            log.info("Getting secret {}...", spotlightConfig.getSecretName());
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                    .secretId(spotlightConfig.getSecretName())
                    .build();
            GetSecretValueResponse valueResponse = secretsManagerClient.getSecretValue(valueRequest);
            String secretString = valueResponse.secretString();

            log.info("Secret: {}", secretString);

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
