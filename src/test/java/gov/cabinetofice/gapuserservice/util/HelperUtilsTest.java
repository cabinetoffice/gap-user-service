package gov.cabinetofice.gapuserservice.util;

import gov.cabinetofice.gapuserservice.exceptions.UnauthorizedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HelperUtilsTest {
    @Test
    void shouldRemoveSquareBracketsAndTrimAndReturnASet() {
        List<String> input = List.of("[FIND ", "APPLY", "ADMIN] ");
        Set<String> output = Set.of("FIND", "APPLY", "ADMIN");
        Set<String> result = HelperUtils.removeSquareBracketsAndTrim(input);
        assertThat(result.equals(output));
    }

    @Test
    void getCustomJwtCookieFromRequestShouldReturnCookie() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getCookies()).thenReturn(new Cookie[]{new Cookie("cookieName", "cookieValue")});

        final Cookie cookie = HelperUtils.getCustomJwtCookieFromRequest(httpRequest, "cookieName");

        assertEquals("cookieName", cookie.getName());
        assertEquals("cookieValue", cookie.getValue());
    }

    @Test
    void getCustomJwtCookieFromRequestShouldThrowException() {
        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getCookies()).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> HelperUtils.getCustomJwtCookieFromRequest(httpRequest, "cookieName"));
    }

}
