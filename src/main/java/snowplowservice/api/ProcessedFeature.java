package snowplowservice.api;

import java.util.Map;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

@Getter
public class ProcessedFeature {

    private final Geometry geometry;

    private final LocalDateTime start;

    private final boolean isOld; // snow-plowed more than 3 hours ago

    public ProcessedFeature(LineString rawFeature) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        final Map<String, Object> userData = (Map<String, Object>) rawFeature.getUserData();
        LocalDateTime dateTime = LocalDateTime.parse(userData.get("start").toString().split("\\+")[0], formatter);
        this.geometry = rawFeature;
        this.start = dateTime;
        LocalDateTime now = LocalDateTime.now();
        this.isOld = start.isBefore(now.minusHours(3));
    }
}
