package gov.cabinetofice.gapuserservice.util;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class WebUtilTest {

    private static final String MANAGE_NOTIFICATIONS_URL = "http://localhost:3002/notifications/manage-notifications";

    @Test()
    void testConstructorIsPrivate() throws NoSuchMethodException {
        Constructor<WebUtil> constructor = WebUtil.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    @Test
    void buildCookie_SetsSecureAndHttpOnly() {
        final Cookie cookie = new Cookie("userId", "1");

        assertThat(cookie.isHttpOnly()).isFalse();
        assertThat(cookie.getSecure()).isFalse();

        final Cookie methodResponse = WebUtil.buildCookie(cookie, true, true, null);

        assertThat(methodResponse.isHttpOnly()).isTrue();
        assertThat(methodResponse.getSecure()).isTrue();
        assertThat(methodResponse).isSameAs(cookie);
    }

    @Test
    void shouldReturnValidUrlWhenUrlInputDoesNotContainQueryParam() {
        List<String> queryParams = Arrays.asList(
                "applyMigrationStatus=ALREADY_MIGRATED",
                "findMigrationStatus=SUCCEEDED");

        String result = WebUtil.parseUrlRequestParameters(MANAGE_NOTIFICATIONS_URL, queryParams);

        assertEquals(MANAGE_NOTIFICATIONS_URL +
                "?applyMigrationStatus=ALREADY_MIGRATED&findMigrationStatus=SUCCEEDED", result);
    }

    @Test
    void shouldReturnValidUrlWhenUrlInputDoesContainQueryParam() {
        List<String> queryParams = Arrays.asList(
                "applyMigrationStatus=ALREADY_MIGRATED",
                "findMigrationStatus=SUCCEEDED");

        String result = WebUtil.parseUrlRequestParameters(MANAGE_NOTIFICATIONS_URL + "?action=subscribe",
                queryParams);

        assertEquals(MANAGE_NOTIFICATIONS_URL +
                "?action=subscribe&applyMigrationStatus=ALREADY_MIGRATED&findMigrationStatus=SUCCEEDED", result);
    }
}