package gov.cabinetoffice.gapuserservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtHeader {

    @JsonProperty("kid")
    private String kid;

    @JsonProperty("typ")
    private String typ;

    @JsonProperty("alg")
    private String alg;
}
