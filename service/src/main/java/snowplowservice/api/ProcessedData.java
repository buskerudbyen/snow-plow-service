package snowplowservice.api;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ProcessedData {

    private List<ProcessedFeature> features;

    @Setter
    private boolean isSnowing;

    public ProcessedData() {
        this.features = new ArrayList<>();
    }

    public void addFeatures(List<ProcessedFeature> features) {
        this.features = new ArrayList<>();
        this.features.addAll(features);
    }
}
