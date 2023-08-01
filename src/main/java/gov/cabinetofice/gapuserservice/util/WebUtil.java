package gov.cabinetofice.gapuserservice.util;

import jakarta.servlet.http.Cookie;

public class WebUtil {

    private WebUtil() {
        throw new IllegalStateException("Not allowed");
    }

    public static Cookie buildCookie(final Cookie cookie, final boolean isSecure, final boolean isHttpOnly, final String domain) {
        cookie.setSecure(isSecure);
        cookie.setHttpOnly(isHttpOnly);
        cookie.setPath("/");

        if (domain != null) {
            cookie.setDomain(domain);
        }

        return cookie;
    }

    public static Cookie buildSecureCookie(final String name, final String value) {
        final Cookie cookie = new Cookie(name, value);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }
}
