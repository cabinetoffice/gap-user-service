package gov.cabinetofice.gapuserservice.util;

import jakarta.servlet.http.Cookie;

public class WebUtil {

    private WebUtil() {
        throw new RuntimeException("Not allowed");
    }

    public static Cookie buildCookie(final Cookie cookie, final boolean isSecure, final boolean isHttpOnly) {
        cookie.setSecure(isSecure);
        cookie.setHttpOnly(isHttpOnly);
        //TODO add functionality to capture domain

        return cookie;
    }
}
