package snowplowservice.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Getter
public class RawData {

    @JsonProperty
    private String type;

    @JsonProperty
    private List<RawFeature> features;
}
