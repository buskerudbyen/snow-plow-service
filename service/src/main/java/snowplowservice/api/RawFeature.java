package snowplowservice.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@Getter
public class RawFeature {

    @JsonProperty
    private String type;

    @JsonProperty
    private RawGeometry geometry;

    @JsonProperty
    private Map<String, Object> properties;

}
