package gov.cabinetofice.gapuserservice.util;

import jakarta.servlet.http.Cookie;

public class WebUtil {

    private WebUtil() {
        throw new IllegalStateException("Not allowed");
    }

    public static Cookie buildCookie(final Cookie cookie, final boolean isSecure, final boolean isHttpOnly) {
        cookie.setSecure(isSecure);
        cookie.setHttpOnly(isHttpOnly);

        return cookie;
    }
}
