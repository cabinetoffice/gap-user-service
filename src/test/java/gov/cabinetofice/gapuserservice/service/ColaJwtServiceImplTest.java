//package gov.cabinetofice.gapuserservice.service;
//
//import ch.qos.logback.classic.Level;
//import ch.qos.logback.classic.Logger;
//import ch.qos.logback.classic.spi.ILoggingEvent;
//import ch.qos.logback.core.read.ListAppender;
//import com.auth0.jwk.JwkProvider;
//import gov.cabinetofice.gapuserservice.config.ThirdPartyAuthProviderProperties;
//import gov.cabinetofice.gapuserservice.exceptions.JwkNotValidTokenException;
//import static javax.crypto.Mac.getInstance;
//import org.apache.commons.codec.digest.DigestUtils;
//import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
//import static org.junit.jupiter.api.Assertions.*;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import static org.mockito.ArgumentMatchers.anyString;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.Mockito;
//import static org.mockito.Mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.slf4j.LoggerFactory;
//
//import javax.crypto.Mac;
//import java.nio.charset.StandardCharsets;
//import java.security.NoSuchAlgorithmException;
//import java.util.List;
//
//@ExtendWith(MockitoExtension.class)
//public class ColaJwtServiceImplTest {
//
//    @Mock
//    private JwkProvider jwkProvider;
//    private ColaJwtServiceImpl serviceUnderTest;
//
//    private final String sampleJwt = "s:eyJraWQiOiJoRG5mWTR1QVpjS3Z5TlZVNFZVN0ZvRU41STBCT0FpQVwvRTU3Yll6cHY4Zz0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhYzA5ZTBiOS1mMjhhLTRlNWItOGM4Yi0yY2I3YjYwMTYzMjMiLCJjdXN0b206bGFzdExvZ2luIjoiMjAyMy0wMi0xNVQxMDoxNjoyNi43MzhaIiwiY3VzdG9tOmZlYXR1cmVzIjoidXNlcj1vcmRpbmFyeV91c2VyIiwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLmV1LXdlc3QtMi5hbWF6b25hd3MuY29tXC9ldS13ZXN0LTJfVldXWW5ZUXBIIiwiY29nbml0bzp1c2VybmFtZSI6ImRvbWluaWMud2VzdEBhbmQuZGlnaXRhbCIsImdpdmVuX25hbWUiOiJEb21pbmljIiwiYXVkIjoiN285Nm8xOW01OTI3czI5amhxMjU5ZjlsMWkiLCJldmVudF9pZCI6IjQ5Y2FlZDU5LWJiMjgtNGQ0MC1iMzI0LTgwYmZmNGFhYTkwYSIsInRva2VuX3VzZSI6ImlkIiwiY3VzdG9tOnBob25lTnVtYmVyIjoiKzQ0NzgyMzM4NjEzNiIsImF1dGhfdGltZSI6MTY3NjQ2OTk2OSwiZXhwIjoxNjc2NDczNTY5LCJpYXQiOjE2NzY0Njk5NjksImZhbWlseV9uYW1lIjoiV2VzdCIsImVtYWlsIjoiZG9taW5pYy53ZXN0QGFuZC5kaWdpdGFsIiwiY3VzdG9tOmlzQWRtaW4iOiJmYWxzZSJ9.MtSzX5TExNJ1PBD6xDd1MVNlraRkPydYBvBCVqQ3rQOr-FsJg1wHHm1c_8wpLjxRVTnSIHgimyu0sPF1lhzRtDJTzB_31wxt70fSCa3ioEE9JVsejY5xdLj4wbC2nEs_HBNRa2j3nxfIl63gWJ-rh2e3B7mdSV5prKY48PEmLCgzWteT5ZfFYLd1xREOBSRR94x_Zzll7vnvNxrZTHV0goiSZBVWlLoUtA3EpB6Lz7q727GuZS2wmG-vgIMFionmnKzwnB-2g9D2ev75_IRDb2dFN6xFqs8GRQy9MIVPPFjl68mhVdfpULNkDTq2DFlbz9HzD4LO3nv_A485MXLIsg.SB2PoJxzaHoUGC2Zk4uHUsZQT9dOaWxdtbla0laCvYE";
//    private final String sampleNonExpiredJwt = "s:eyJraWQiOiJoRG5mWTR1QVpjS3Z5TlZVNFZVN0ZvRU41STBCT0FpQS9FNTdiWXpwdjhnPSIsImFsZyI6IkhTMjU2In0.eyJzdWIiOiJhYzA5ZTBiOS1mMjhhLTRlNWItOGM4Yi0yY2I3YjYwMTYzMjMiLCJjdXN0b206bGFzdExvZ2luIjoiMjAyMy0wMi0xNVQxMDoxNjoyNi43MzhaIiwiY3VzdG9tOmZlYXR1cmVzIjoidXNlcj1vcmRpbmFyeV91c2VyIiwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC5ldS13ZXN0LTIuYW1hem9uYXdzLmNvbS9ldS13ZXN0LTJfVldXWW5ZUXBIIiwiY29nbml0bzp1c2VybmFtZSI6ImRvbWluaWMud2VzdEBhbmQuZGlnaXRhbCIsImdpdmVuX25hbWUiOiJEb21pbmljIiwiYXVkIjoiN285Nm8xOW01OTI3czI5amhxMjU5ZjlsMWkiLCJldmVudF9pZCI6IjQ5Y2FlZDU5LWJiMjgtNGQ0MC1iMzI0LTgwYmZmNGFhYTkwYSIsInRva2VuX3VzZSI6ImlkIiwiY3VzdG9tOnBob25lTnVtYmVyIjoiKzQ0NzgyMzM4NjEzNiIsImF1dGhfdGltZSI6MTY3NjQ2OTk2OSwiZXhwIjo5OTk5OTk5OTk5LCJpYXQiOjE2NzY0Njk5NjksImZhbWlseV9uYW1lIjoiV2VzdCIsImVtYWlsIjoiZG9taW5pYy53ZXN0QGFuZC5kaWdpdGFsIiwiY3VzdG9tOmlzQWRtaW4iOiJmYWxzZSJ9.oOzq3qNwD3CB0CyLVqWgHLYIro-PX97DbeVl_Ve9VvA.colaSignature";
//
//    @BeforeEach
//    void setup() {
//        final ThirdPartyAuthProviderProperties thirdPartyAuthProviderProperties = ThirdPartyAuthProviderProperties.builder()
//                .accessKey("an-access-key")
//                .secretKey("a-secret-key")
//                .userPoolId("a-user-pool-id")
//                .region("eu-west-2")
//                .userPassword("a-user-password")
//                .domain("domain")
//                .appClientId("appClientId")
//                .secretCookieKey("secretCookieKey")
//                .build();
//
//        serviceUnderTest = new ColaJwtServiceImpl(thirdPartyAuthProviderProperties, jwkProvider);
//    }
//
//    @Test
//    void verifyToken_expiredToken() {
//        try (MockedStatic<DigestUtils> utilities = Mockito.mockStatic(DigestUtils.class)) {
//            utilities.when(() -> sha1Hex(anyString())).thenReturn("mockedHash");
//            final boolean methodResponse = serviceUnderTest.verifyToken(sampleJwt);
//            assertFalse(methodResponse);
//        }
//    }
//
//    @Test
//    void verifyToken_ThrowErrorWhenNotExpectedIssuerOrAudience() {
//        try (MockedStatic<DigestUtils> utilities = Mockito.mockStatic(DigestUtils.class)) {
//            utilities.when(() -> sha1Hex(anyString())).thenReturn("mockedHash");
//            final Exception result = assertThrows(JwkNotValidTokenException.class,
//                    () -> serviceUnderTest.verifyToken(sampleNonExpiredJwt));
//            assertTrue(result.getMessage().contains("Third party token is not valid"));
//        }
//    }
//
//    @Test
//    void verifyToken_InvalidColaSignature() {
//        final Logger logger = (Logger) LoggerFactory.getLogger(ColaJwtServiceImpl.class);
//        final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
//        listAppender.start();
//        logger.addAppender(listAppender);
//        final List<ILoggingEvent> logsList = listAppender.list;
//
//        serviceUnderTest.verifyToken(sampleJwt);
//        assertEquals("COLAs JWT signature is invalid", logsList.get(0)
//                .getMessage());
//        assertEquals(Level.ERROR, logsList.get(0)
//                .getLevel());
//    }
//
//    @Test
//    void verifyToken_ColaSignatureCallsRightAlgorithm() {
//        try (MockedStatic<DigestUtils> utilities = Mockito.mockStatic(DigestUtils.class)) {
//            try (MockedStatic<Mac> staticMac = Mockito.mockStatic(Mac.class)) {
//                utilities.when(() -> sha1Hex(anyString())).thenReturn("mockedHash");
//                Mac mac = mock(Mac.class);
//                when(mac.doFinal(any())).thenReturn(new byte[]{});
//                staticMac.when(() -> getInstance(anyString())).thenReturn(mac);
//
//                assertThrows(JwkNotValidTokenException.class,
//                        () -> serviceUnderTest.verifyToken(sampleNonExpiredJwt));
//
//                utilities.verify(() -> sha1Hex(new String(sampleNonExpiredJwt
//                                .getBytes(StandardCharsets.UTF_8))
//                                .substring(2)),
//                        times(1));
//                verify(mac, times(1)).doFinal(any());
//                staticMac.verify(() -> getInstance("HmacSHA256"), times(1));
//                // TODO verify secret added to mac?
//            }
//        }
//    }
//
//    @Test
//    void verifyToken_InvalidSecretCookie() {
//        final Logger logger = (Logger) LoggerFactory.getLogger(ColaJwtServiceImpl.class);
//        final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
//        listAppender.start();
//        logger.addAppender(listAppender);
//        final List<ILoggingEvent> logsList = listAppender.list;
//        final ThirdPartyAuthProviderProperties thirdPartyAuthProviderProperties = ThirdPartyAuthProviderProperties.builder()
//                .secretCookieKey("")
//                .build();
//        serviceUnderTest = new ColaJwtServiceImpl(thirdPartyAuthProviderProperties);
//
//
//        assertThrows(RuntimeException.class,
//                () -> serviceUnderTest.verifyToken(sampleJwt));
//        assertEquals("Invalid secret COLA cookie key provided", logsList.get(0)
//                .getMessage());
//        assertEquals(Level.ERROR, logsList.get(0)
//                .getLevel());
//    }
//
//    @Test
//    void verifyToken_InvalidHashingAlgorithm() {
//        final Logger logger = (Logger) LoggerFactory.getLogger(ColaJwtServiceImpl.class);
//        final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
//        listAppender.start();
//        logger.addAppender(listAppender);
//        final List<ILoggingEvent> logsList = listAppender.list;
//
//        try (MockedStatic<Mac> mac = Mockito.mockStatic(Mac.class)) {
//            mac.when(() -> getInstance(anyString())).thenThrow(new NoSuchAlgorithmException());
//
//            assertThrows(RuntimeException.class,
//                    () -> serviceUnderTest.verifyToken(sampleJwt));
//            assertEquals("Invalid COLA hashing algorithm used", logsList.get(0)
//                    .getMessage());
//            assertEquals(Level.ERROR, logsList.get(0)
//                    .getLevel());
//        }
//    }
//
//    @Test
//    void verifyToken_InvalidJwtDecoding() {
//
//    }
//
//    @Test
//    void verifyToken_InvalidJwtSignature() {
//
//    }
//
//    @Test
//    void verifyToken_HappyPath() {
//
//    }
//}
