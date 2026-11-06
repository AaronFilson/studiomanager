package io.github.aaronfilson.studiomanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class StudioManagerApplication {

  public static void main(String[] args) {
    ConfigurableApplicationContext context =
        SpringApplication.run(StudioManagerApplication.class, args);
    // The migrate and sweep profiles have no web server: Flyway, or every sweep (SweepJob), ran
    // during startup, so the job is done. The exit code says whether it succeeded.
    if (context.getEnvironment().matchesProfiles("migrate | sweep")) {
      System.exit(SpringApplication.exit(context));
    }
  }
}
