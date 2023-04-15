package snowplowservice.api;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ProcessedData {

    private final List<ProcessedFeature> features;

    @Setter
    private boolean isSnowing;

    public ProcessedData() {
        this.features = new ArrayList<>();
    }

    public void addFeature(ProcessedFeature feature) {
        features.add(feature);
    }
}
