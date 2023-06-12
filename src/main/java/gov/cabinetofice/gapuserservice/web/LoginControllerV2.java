package gov.cabinetofice.gapuserservice.web;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;


@RequiredArgsConstructor
@Controller
@RequestMapping("v2")
public class LoginControllerV2 {

    private static final String CLIENT_ID = "dwYu15OryLnH_BGVXUfQI3Uc37U";
    private static final String GRANT_TYPE = "authorization_code";
    private static final String ONE_LOGIN_BASE_URL = "https://oidc.integration.account.gov.uk";
    private static final String CLIENT_ASSERTION_TYPE = "urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer";
    private static final String SERVICE_REDIRECT_URI = "https://sandbox-gap.service.cabinetoffice.gov.uk" +
            "/apply/user/v2/redirect-after-login";

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @GetMapping("/redirect-after-login")
    public ResponseEntity<String> login(final @RequestParam String code) {

        String jwt = createOneLoginJwt();
        String authToken = getAuthToken(jwt, code);
        String userInfo = getUserInfo(authToken);

        return ResponseEntity.ok(userInfo);
    }

    private String getPrivateKeyString() {

        // TODO: Use secret manager instead
        String filePath = "src/main/resources/key.txt";
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private PrivateKey parsePrivateKey(String privateKeyString) {
        try {
            //TODO: Potentially refactor this method

            // Read String as one line
            StringBuilder sb = new StringBuilder();
            BufferedReader rdr = new BufferedReader(new StringReader(privateKeyString));
            String line;
            while ((line = rdr.readLine()) != null) {
                sb.append(line);
            }

            // Remove the "BEGIN" and "END" lines, as well as any whitespace

            String keyString = sb.toString();
            keyString = keyString.replace("-----BEGIN PRIVATE KEY-----", "");
            keyString = keyString.replace("-----END PRIVATE KEY-----", "");
            keyString = keyString.replaceAll("\\s+","");

            // Base64 decode the result

            byte [] pkcs8EncodedBytes = Base64.getDecoder().decode(keyString);

            // extract the private key

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // TODO Throw Exception here
        return null;
    }


    public String createOneLoginJwt() {

        PrivateKey privateKey = parsePrivateKey(getPrivateKeyString());

        return Jwts.builder()
                .claim("aud", ONE_LOGIN_BASE_URL + "/token")
                .claim("iss", CLIENT_ID)
                .claim("sub", CLIENT_ID)
                .claim("exp", System.currentTimeMillis() + 300000L)
                .claim("jti", UUID.randomUUID().toString())
                .claim("iat", System.currentTimeMillis())
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String getAuthToken(String jwt, String code) {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(ONE_LOGIN_BASE_URL + "/token");
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

        String requestBody = "grant_type=" + GRANT_TYPE +
                "&code=" + code +
                "&redirect_uri=" + SERVICE_REDIRECT_URI +
                "&client_assertion_type=" + CLIENT_ASSERTION_TYPE +
                "&client_assertion=" + jwt;

        try {
            httpPost.setEntity(new StringEntity(requestBody));
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();

            String responseBody = EntityUtils.toString(responseEntity);
            JSONObject jsonObject = new JSONObject(responseBody);

            //TODO: Throw exception if access_token doesn't exist

            return jsonObject.getString("access_token");

        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO Throw Exception here
        return null;
    }

    public String getUserInfo(String accessToken) {
        HttpClient httpClient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(ONE_LOGIN_BASE_URL + "/userinfo");
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        try {

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            JSONObject jsonResponse = new JSONObject(responseBody);

            return jsonResponse.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO Throw Exception here
        return null;
    }

}