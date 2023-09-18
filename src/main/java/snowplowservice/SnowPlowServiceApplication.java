package snowplowservice;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import snowplowservice.resources.SnowPlowResource;
import io.dropwizard.setup.Environment;

public class SnowPlowServiceApplication extends Application<SnowPlowServiceConfiguration> {

    public static void main(final String[] args) throws Exception {
        new SnowPlowServiceApplication().run(args);
    }

    @Override
    public String getName() {
        return "DataProcessor";
    }

    @Override
    public void initialize(final Bootstrap<SnowPlowServiceConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final SnowPlowServiceConfiguration configuration,
                    final Environment environment) {
        final SnowPlowResource resource = new SnowPlowResource();
        environment.jersey().register(resource);
    }

}
