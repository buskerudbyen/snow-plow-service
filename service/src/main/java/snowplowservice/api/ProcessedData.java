package snowplowservice.api;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ProcessedData {

    private final List<ProcessedFeature> features;

    public ProcessedData() {
        this.features = new ArrayList<>();
    }

    public void addFeature(ProcessedFeature feature) {
        features.add(feature);
    }
}
