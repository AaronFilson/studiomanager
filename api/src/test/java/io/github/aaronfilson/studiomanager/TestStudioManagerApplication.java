package io.github.aaronfilson.studiomanager;

import org.springframework.boot.SpringApplication;

/**
 * Runs the API against a throwaway PostgreSQL container instead of compose.yaml:
 * {@code ./gradlew bootTestRun}. Needs Docker and the STUDIOMANAGER_AUTH_* variables.
 */
public class TestStudioManagerApplication {

    public static void main(String[] args) {
        SpringApplication.from(StudioManagerApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
