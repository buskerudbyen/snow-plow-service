package snowplowservice.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class RawGeometry {

    @JsonProperty
    private String type;

    @JsonProperty
    private LinkedList<List<Object>> coordinates;

}
