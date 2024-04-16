package gov.cabinetoffice.gapuserservice.security.interceptors;


import gov.cabinetoffice.gapuserservice.annotations.ServiceToServiceHeaderValidation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.method.HandlerMethod;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
class AuthorizationHeaderInterceptorTest {

    private static final String EXPECTED_AUTHORIZATION_VALUE = "expectedToken";

    private static final String ENCRYPTED_EXPECTED_AUTHORIZATION_VALUE = "gPiAtfeQbMcv9e0boGa2tlNIthMM8uXq1JnzQy/KVPtpcmqVpOkA28dKR7UN/hJTO8ACW3TVVMTzHxvmJT+YPUmC7ggRHO9VSpItdPBdwgaHzDDu571KYrTLKEeUYhzt00PfB+O7kwA8cw1sr2rog8dI0wXVU0tBqkoFjBUl+NdSCe/AxkjL1ziq6MSpI3yflJC3crkYoZf0zrWQTKRZWIr2MXBgCRHfXGy0LKdwrkvKUUvcWObSybA6yPFzGW4IDNbivOUngGr9goRtnkSY3/ezvsQzl/nPGS2VF2FrTtDD/U7kKOE83s5ojKR8VEu4hCieYu0T8eT3ur51seHTOA==";

    private static final String PRIVATE_KEY = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDUKDTCzH6/coqqf2y95NWJ/FfrSbT273rCrTT8in+CDeDaHmm+kHSwOigL5o5Vci4kY71/pqLASp9PwmSbbWuzTnK0hr30aZsYELVr1Nv4lE6IrQUby2pDaiXdd/PrKBeXGbzd5JNmamYqLn8B9jl3Sftde66pGhimaSr2MTLGfE32ZoK8VA3mAHGURt9zWZ3B97d+ivkT/UTu64VzczsyrIf9Rtll0xrIki00NToXt5gX32YPzGHX1ZYtT2aGVpfZSpgnwOuJZORIepE8aB5jsqvy24gGBi60nYFSXW1mxVRJ7VHZoOLPkiX28BLexsl4CIYTvAUwJhEGy3QxOTWRAgMBAAECggEAD6unmALkUs5KVUUjulQCfNZ1hQTU989hDSufiA7P08KkKuBHtcJ0SAcHqbdeKIwOQfRXDSx5BYSNh2Xnqf5XT+95tAFm2vyI7upfCCe5Fr+XnGtRPwgToYaf5N+lFefIdEn7pBUr2Qr/YFq+WV3/SSMfDLzjvgxWKUaH3JbuNfBafyuLDObXFYKTxS+JJIE/DHFKK9qG+D1KSaJsm7XxD3u9njziXiNP62DnnhpW7NE7z8Bj8Pn5qJEnW8DSjGlWfeNJ0dvXvepBgilk0dZKEuDXVHpw3yxl/aP+SWA3IxMsYRi6vGC6dCASO1oiQoh69FhWAxYeyZAFJXM+5luH1QKBgQD+eq26WN7HkD9slrvA9lP7NaE47BsFsxJbWGjJOkimA+arVVvKlflPZ8RR4nQIFex8ygm2LoTZVxLb6y21NVowNthPltitWPEuvGxmwM2dP9wHU+HGo+TSFVTKspxj9vdBItO2Jv9xatevUjbmmkJwUero3pAn7yss6OkzBx1rnQKBgQDVbMelE/Bj6yFtFFIGuljpap6R3gMEq7+cqIvYT5z1EOdb23DPYD30U7YZPx19hIB+Q8NF6CHiuLIgNgm4Vm0DlCBaxuS/SvXclII/hMKOTKFI8TkQ4C3cOukMpbrZRSnslfQAx9Nw2I6aega8MmQx+Ib3oX97K4utbDKzGTBxhQKBgDS0KajPYQxJtqkBqs7y9T+wwrwsQghzJtkePU13sbYYVryjSAhz+RdV8VXYJZnLGJhbq5k8ly0AGJrNYUEHVK9pnHEXV7bHFeRNB9JcGfl4UVR/LeJa7TgJTO9SAIu/iUPHN7ug5gSSUSsxRseJqTrj7FfgSFDM+s8pSarUzWYNAoGAZv0UJhPGF+FaAvIgVwDLcO+zTz5sBHAAic9HlH1uh0+95TSybk/J9cIDiJFNYMl02/lFHNHUsDxiMGsDK8IA3w42wrdhoPHCTMwZQh+FZveRiMpmuD8FwlVnKmQ7EyduAK2nzkyfOsA1qFOkNmK2uOpt7scu5jfwMiKZIJXSK6UCgYB446Uz1BR/0Gb7zQ6hbV/tEO1pOtgV/uXMOFJ4UOulyQzQXyIhMuzT/66oxKc7Ik1ZQ468bVv4CCL4yoaKOj04j1z23v3aR9tg2gv45ryRiqo2vJbwMrZpP/BYeIZehxMrXauqMKwl/t64JR8XTwArbWVDSIqwYX76kGG46YHW/w==";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HandlerMethod handlerMethod;

    @InjectMocks
    private AuthorizationHeaderInterceptor authorizationHeaderInterceptor;

    @Test
    void preHandleValidAuthorization() throws Exception {
        final byte[] expectedAuthorizationValueBytes = EXPECTED_AUTHORIZATION_VALUE.getBytes(StandardCharsets.UTF_8);

        authorizationHeaderInterceptor = new AuthorizationHeaderInterceptor(EXPECTED_AUTHORIZATION_VALUE, PRIVATE_KEY);
        when(handlerMethod.getMethod()).thenReturn(getClass().getMethod("annotatedTestMethod"));
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(ENCRYPTED_EXPECTED_AUTHORIZATION_VALUE);

        boolean result = authorizationHeaderInterceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
    }

    @Test
    void preHandleNullAuthorizationHeader() throws Exception {
        when(handlerMethod.getMethod()).thenReturn(getClass().getMethod("annotatedTestMethod"));
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        boolean result = authorizationHeaderInterceptor.preHandle(request, response, handlerMethod);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertFalse(result);
    }

    @Test
    void preHandleInvalidAuthorizationHeader() throws Exception {
        String invalidAuthorizationValue = "invalidToken";
        when(handlerMethod.getMethod()).thenReturn(getClass().getMethod("annotatedTestMethod"));
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(invalidAuthorizationValue);

        boolean result = authorizationHeaderInterceptor.preHandle(request, response, handlerMethod);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertFalse(result);
    }

    @Test
    void preHandleValidAuthorizationForNonAnnotatedMethods() throws Exception {
        authorizationHeaderInterceptor = new AuthorizationHeaderInterceptor(EXPECTED_AUTHORIZATION_VALUE, PRIVATE_KEY);
        when(handlerMethod.getMethod()).thenReturn(getClass().getMethod("nonAnnotatedTestMethod"));
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(EXPECTED_AUTHORIZATION_VALUE);

        boolean result = authorizationHeaderInterceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
    }

    // Test method to provide a valid HandlerMethod for testing
    @ServiceToServiceHeaderValidation
    public void annotatedTestMethod() {
        // This method is just a placeholder for testing HandlerMethod
    }

    // Test method to provide a NON valid HandlerMethod for testing
    public void nonAnnotatedTestMethod() {
        // This method is just a placeholder for testing HandlerMethod
    }

}