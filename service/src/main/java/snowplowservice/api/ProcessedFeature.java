package snowplowservice.api;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
public class ProcessedFeature {

    private final RawGeometry geometry;

    private final LocalDateTime start;

    private final boolean isOld; // snow-plowed more than 3 hours ago

    public ProcessedFeature(RawFeature rawFeature) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(rawFeature.getProperties().get("start").toString().split("\\+")[0], formatter);
        this.geometry = rawFeature.getGeometry();
        this.start = dateTime;
        LocalDateTime now = LocalDateTime.now();
        this.isOld = start.isBefore(now.minusHours(3));
    }
}
