package snowplowservice.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.locationtech.jts.geom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.jts2geojson.GeoJSONReader;
import org.wololo.jts2geojson.GeoJSONWriter;
import snowplowservice.api.ProcessedData;
import snowplowservice.api.ProcessedFeature;

import javax.annotation.Nonnull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Path("/snow-plow-konnerudgata")
@Produces(MediaType.APPLICATION_JSON)
public class SnowPlowResource {

    Logger LOG = LoggerFactory.getLogger(SnowPlowResource.class);

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

    private final LoadingCache<String, String> cache;

    public SnowPlowResource() {
        this.konnerudgataArea = new GeometryFactory().createPolygon(konnerrudgataAreaCoords);
        this.cache = CacheBuilder.newBuilder()
                .refreshAfterWrite(9, TimeUnit.MINUTES)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(new CacheLoader<String, String>() {
                            @Override
                            public String load(final @Nonnull String s) {
                                if ("konnerudgata".equals(s)) {
                                    return loadData();
                                }
                                return null;
                            }
                        }
                      );
    }

    @GET
    @Timed
    public String getSnowPlowData() {
        try {
            return cache.get("konnerudgata");
        } catch (ExecutionException e) {
            return "ERROR";
        }
    }

    private String loadData() {
        List<Geometry> geometries = getGeometries();
        List<LineString> lineStrings =
                geometries.stream().filter(LineString.class::isInstance).map(LineString.class::cast).toList();
        List<Point> points =
                geometries.stream().filter(Point.class::isInstance).map(Point.class::cast).toList();

        ProcessedData processedData = new ProcessedData();
        if (!points.isEmpty()) {
            processedData.setSnowing(getSnowInfo(points));
        }

        if (!lineStrings.isEmpty()) {
            processedData.addFeatures(filterData(lineStrings));
        }

        return writeGeojson(processedData);
    }

    private String getStaticData(String fileName) {
        InputStream resource = SnowPlowResource.class.getResourceAsStream(fileName);

        try {
            if (resource == null) {
                throw new IllegalArgumentException(fileName);
            } else {
                return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IllegalArgumentException | IOException e) {
            LOG.error("Error getting the data from the static file.", e);
            throw new RuntimeException(e);
        }
    }

    private URL constructUrl() throws MalformedURLException {
        try {
            String url = System.getenv("DATA_URL");
            return new URL(url);
        } catch (MalformedURLException e) {
            LOG.error("Error creating the URL.", e);
            throw e;
        }
    }

    private List<Geometry> getGeometries() {
        try {
            var req = HttpRequest.newBuilder(constructUrl().toURI()).build();
            var response = HttpClient.newHttpClient().send(req, BodyHandlers.ofString());
            var geojson = GeoJSONFactory.create(response.body());

            if(geojson instanceof FeatureCollection fc) {
                return Arrays.stream(fc.getFeatures()).map(f -> {
                   GeoJSONReader reader = new GeoJSONReader();
                   var g = reader.read(f.getGeometry());
                   g.setUserData(f.getProperties());
                   return g;
                }).collect(Collectors.toList());
            }
            else {
                return List.of();
            }

        } catch (IOException | URISyntaxException | InterruptedException e) {
            LOG.error("Error getting the data from the URL.", e);
            throw new RuntimeException(e);
        }
    }

    private List<ProcessedFeature> filterData(List<LineString> lineStrings) {
        List<ProcessedFeature> features = new ArrayList<>();

        for (LineString lineString : lineStrings) {
            // Keep lines that are in the Konnerrudgata area.
            if (konnerudgataArea.contains(lineString)) {
                features.add(new ProcessedFeature(lineString));
            }
        }

        return features;
    }

    private boolean getSnowInfo(List<Point> points) {
        boolean currentlySnowing = false;

        // should be 1 Point with weather details
        Point weatherInfo = points.get(0);
        if (weatherInfo != null && weatherInfo.getUserData() != null) {
            currentlySnowing = ((LinkedHashMap<String, Object>) weatherInfo.getUserData())
                    .get("weather_status").toString().contains("snow");
        }

        return currentlySnowing;
    }

    private String writeGeojson(ProcessedData data) {
        GeoJSONWriter writer = new GeoJSONWriter();
        boolean allOld = true;
        List<Feature> featureList = new ArrayList<>();

        for (ProcessedFeature feature : data.getFeatures()) {
            allOld &= feature.isOld();
            featureList.add(createGeoJsonFeature(writer, feature));
        }

        if (allOld && data.isSnowing()) {
            return getStaticData("/assets/konnerudgata-snowing.geojson");
        }

        // no snow-plowing data available, so we assume that it has been more than 3 hours since
        // the last plow and we want to show an all orange line
        if (data.getFeatures().isEmpty()) {
            return getStaticData("/assets/konnerudgata-linestring.geojson");
        }

        FeatureCollection collection = writer.write(featureList);
        return collection.toString();
    }

    private Feature createGeoJsonFeature(GeoJSONWriter writer, ProcessedFeature processedFeature) {
        org.wololo.geojson.Geometry geometry = writer.write(processedFeature.getGeometry());

        Map<String, Object> properties = new HashMap<>();
        properties.put("isOld", processedFeature.isOld());

        return new Feature(geometry, properties);
    }
}
