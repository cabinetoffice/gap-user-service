package gov.cabinetoffice.gapuserservice.security.interceptors;

import gov.cabinetoffice.gapuserservice.annotations.ServiceToServiceHeaderValidation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

//this is needed to "authenticate" all the call from other "internal" services
@Log4j2
public class AuthorizationHeaderInterceptor implements HandlerInterceptor {

    private final String expectedAuthorizationValue;

    private final String privateKey;

    public AuthorizationHeaderInterceptor(String expectedAuthorizationValue, String privateKey) {
        this.expectedAuthorizationValue = expectedAuthorizationValue;
        this.privateKey = privateKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("Intercepting request: " + request.getRequestURI());
        if (handler instanceof HandlerMethod handlerMethod) {
            final Method method = handlerMethod.getMethod();

            final ServiceToServiceHeaderValidation annotation = method.getAnnotation(ServiceToServiceHeaderValidation.class);

            if (annotation != null) {
                log.info("Request is coming from another internal service, validating authorization header");

                final String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
                final boolean isAuthorizationHeaderCorrect = compareAuthorizationSecret(authorizationHeader,
                        expectedAuthorizationValue, privateKey);
                if (authorizationHeader == null || !isAuthorizationHeaderCorrect) {

                    log.info("Authorization Header Value: " + authorizationHeader
                            + " does not match the expected value");

                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return false;
                }

                log.info("Authorization Header Value matches the expected value");
            }
        }

        return true;
    }

    private boolean compareAuthorizationSecret(String authorizationHeader, String expectedAuthorizationValue,
            String privateKey) {

        if (authorizationHeader == null || privateKey == null) {
            return false;
        }

        return decrypt(authorizationHeader, privateKey).equals(expectedAuthorizationValue);

    }

    private String decrypt(String encryptedText, String privateKeyString) {

        try {

            final byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyString);
            final byte[] encryptedMessageBytes = Base64.getDecoder().decode(encryptedText);

            final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            final PrivateKey rsaPrivateKey = keyFactory.generatePrivate(keySpec);

            final Cipher decryptCipher = Cipher.getInstance("RSA");
            decryptCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);

            final byte[] decryptedMessageBytes = decryptCipher.doFinal(encryptedMessageBytes);

            return new String(decryptedMessageBytes, StandardCharsets.UTF_8);
        }
        catch (IllegalArgumentException | InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException
                | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            log.info("Error decrypting authorization header from other internal service: " + e.getMessage());
            return "";
        }
    }

}
