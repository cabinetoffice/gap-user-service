package gov.cabinetoffice.gapuserservice.util;

import gov.cabinetoffice.gapuserservice.exceptions.InvalidRequestException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Slf4j
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

    public static Cookie buildSecureCookie(final String name, final String domain, final String value) {
        final Cookie cookie = new Cookie(name, value);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setDomain(domain);
        cookie.setPath("/");
        return cookie;
    }

    public static Cookie buildSecureCookie(final String name, final String value, final Integer maxAge) {
        final Cookie cookie = buildSecureCookie(name, value);
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    public static Cookie buildNullCookie(final String name) {
        final Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);
        return cookie;
    }

    public static void deleteCookie(final String name, final HttpServletResponse response) {
        final Cookie cookie = buildNullCookie(name);
        response.addCookie(cookie);
    }

    public static String parseUrlRequestParameters(String inputUrl, List<String> params) {
        try {
            URL url = new URL(inputUrl);
            String separator = (url.getQuery() == null || url.getQuery().isEmpty()) ? "?" : "&";
            return inputUrl + separator + String.join("&", params);

        } catch (MalformedURLException e) {
            throw new InvalidRequestException("Invalid redirect URL: ".concat(inputUrl)
                    .concat(" ").concat(e.getMessage()));
        }
    }

    public static void validateRedirectUrl(final String inputUrl, final String baseUrl) throws MalformedURLException {
        if (inputUrl.endsWith("/404")) {
            return;
        }
        URL url = new URL(inputUrl);
        String inputUrlHost = url.getProtocol() + "://" + url.getHost();
        if (!baseUrl.startsWith(inputUrlHost)) {
            String errorMessage = "Redirect url: " + inputUrl + " does not match host: " + baseUrl;
            log.error(errorMessage);
            throw new InvalidRequestException(errorMessage);
        }
    }
}