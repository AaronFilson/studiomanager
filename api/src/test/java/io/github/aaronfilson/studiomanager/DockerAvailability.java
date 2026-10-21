package io.github.aaronfilson.studiomanager;

import org.testcontainers.DockerClientFactory;

final class DockerAvailability {

    private DockerAvailability() {
    }

    /**
     * Integration tests skip on a machine without Docker. In CI they always run, so a missing
     * Docker fails the build instead of quietly skipping the tests.
     */
    static boolean dockerAvailableOrCi() {
        return System.getenv("CI") != null || DockerClientFactory.instance().isDockerAvailable();
    }

}
