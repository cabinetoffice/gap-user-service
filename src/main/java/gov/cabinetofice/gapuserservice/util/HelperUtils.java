package gov.cabinetofice.gapuserservice.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

public class HelperUtils {

    private HelperUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Used to generate a safely encoded URL, incl. any query params This method will
     * auto-remove any query params where the value is null, to prevent empty request
     * params being added to the URL
     * @param hostUrl the host name
     * @param path the path to the requested resource
     * @param queryParams a map of query params
     * @return an encoded URL
     */
    public static String buildUrl(String hostUrl, String path, MultiValueMap<String, String> queryParams) {
        if (hostUrl == null || path == null) {
            throw new IllegalArgumentException("hostUrl and path cannot be null");
        }

        if (queryParams != null) {
            queryParams.values().removeIf(Objects::isNull);
        }

        return UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(hostUrl)
                .path(path)
                .queryParams(queryParams)
                .build()
                .toUriString();
    }

    public static String asJsonString(final Object obj) {
        try {
            final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();

            // so null values aren't included in the json
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper.writeValueAsString(obj);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}