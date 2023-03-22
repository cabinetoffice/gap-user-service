package gov.cabinetofice.gapuserservice.util;

import jakarta.servlet.http.Cookie;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

class WebUtilTest {

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
}