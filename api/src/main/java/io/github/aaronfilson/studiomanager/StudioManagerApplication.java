package io.github.aaronfilson.studiomanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class StudioManagerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(StudioManagerApplication.class, args);
        // The migrate profile has no web server: Flyway ran during startup, so the job is done.
        if (context.getEnvironment().matchesProfiles("migrate")) {
            System.exit(SpringApplication.exit(context));
        }
    }

}
