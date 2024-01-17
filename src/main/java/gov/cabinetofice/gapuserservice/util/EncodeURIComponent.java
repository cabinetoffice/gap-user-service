package gov.cabinetofice.gapuserservice.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class EncodeURIComponent {
    public static String encodeURI(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replaceAll("\\+", "%20")
                .replaceAll("\\%21", "!")
                .replaceAll("\\%27", "'")
                .replaceAll("\\%28", "(")
                .replaceAll("\\%29", ")")
                .replaceAll("\\%7E", "~");
    }
}