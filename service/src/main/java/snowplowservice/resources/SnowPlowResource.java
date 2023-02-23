package snowplowservice.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import snowplowservice.api.*;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.locationtech.jts.geom.Geometry.TYPENAME_LINESTRING;

@Path("/snow-plow-konnerudgata")
@Produces(MediaType.APPLICATION_JSON)
public class SnowPlowResource {

    private static final Coordinate[] konnerrudgataAreaCoords = {
            new Coordinate(10.200291233574774,59.73962175701382),
            new Coordinate(10.198452562567013,59.74039388333668),
            new Coordinate(10.182823859004628,59.738386317800234),
            new Coordinate(10.181291633165529,59.73661029393753),
            new Coordinate(10.172557945881152,59.73753692685497),
            new Coordinate(10.165969374770327,59.73637863169475),
            new Coordinate(10.15202611963133,59.735451966671945),
            new Coordinate(10.139461867748537,59.734448050579005),
            new Coordinate(10.142526319426707,59.726801854480954),
            new Coordinate(10.150034226040674,59.719308431147454),
            new Coordinate(10.14329243234701,59.72023554358171),
            new Coordinate(10.146510106609469,59.715986067267835),
            new Coordinate(10.15539701647819,59.71830403034957),
            new Coordinate(10.145131103354771,59.73236289775775),
            new Coordinate(10.17271116846402,59.73498862452854),
            new Coordinate(10.183589971924874,59.734216373353775),
            new Coordinate(10.186501201020121,59.736841954574686),
            new Coordinate(10.201670236829528,59.73869518188428),
            new Coordinate(10.200291233574774,59.73962175701382)
    };
    private final Polygon konnerudgataArea;

    public SnowPlowResource() {
        this.konnerudgataArea = new GeometryFactory().createPolygon(konnerrudgataAreaCoords);
    }

    @GET
    @Timed
    public String getSnowPlowData() {
        RawData rawData;
        try {
            rawData = getRawData();
        } catch (IOException e) {
            return "ERROR";
        }
        ProcessedData processedData = filterData(rawData);

        return writeGeojson(processedData);
    }

    private URL constructUrl() throws MalformedURLException {
        try {
            String url = System.getenv("DATA_URL");
            return new URL(url);
        } catch (MalformedURLException e) {
            System.err.println("Error creating the URL.");
            throw e;
        }
    }

    private RawData getRawData() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(constructUrl(), RawData.class);
        } catch (IOException e) {
            System.err.println("Error getting the data from the URL.");
            throw e;
        }
    }

    private ProcessedData filterData(RawData rawData) {
        ProcessedData processedData = new ProcessedData();

        for (RawFeature feature : rawData.getFeatures()) {
            if (TYPENAME_LINESTRING.equals(feature.getGeometry().getType())) {
                LineString line = new GeometryFactory().createLineString(getCoordArray(feature));
                // Keep lines that are in the Konnerrudgata area.
                if (konnerudgataArea.contains(line)) {
                    // We won't need the LineString object anymore, the geometry stays the same,
                    // but the 4. "coordinate" is invalid.
                    feature.getGeometry().getCoordinates().forEach(coords -> coords.remove(3));
                    processedData.addFeature(new ProcessedFeature(feature));
                }
            }
        }

        return processedData;
    }

    private Coordinate[] getCoordArray(RawFeature feature) {
        CoordinateList coords = new CoordinateList();

        for (List<Object> coordinate : feature.getGeometry().getCoordinates()) {
            Coordinate coord = new Coordinate((double) coordinate.get(0), (double) coordinate.get(1),
                                              (double) (int) coordinate.get(2));
            coords.add(coord);
        }

        return coords.toCoordinateArray();
    }

    private String writeGeojson(ProcessedData data) {
        List<Map<String, Object>> featureList = new ArrayList<>();
        for (ProcessedFeature feature : data.getFeatures()) {
            featureList.add(createGeoJsonFeature(feature));
        }

        return createGeoJsonCollection(featureList);
    }

    private String createGeoJsonCollection(List<Map<String, Object>> featureList) {
        Map<String, Object> collection = new LinkedHashMap<>();
        collection.put("type", "FeatureCollection");
        collection.put("features", featureList);
        return JSONObject.toJSONString(collection);
    }

    private Map<String, Object> createGeoJsonFeature(ProcessedFeature processedFeature) {
        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");

        LinkedHashMap<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("type", processedFeature.getGeometry().getType());
        geometry.put("coordinates", processedFeature.getGeometry().getCoordinates());
        feature.put("geometry", geometry);

        Map<String, Object> properties = new HashMap<>();
        properties.put("isOld", processedFeature.isOld());
        feature.put("properties", properties);

        return feature;
    }
}