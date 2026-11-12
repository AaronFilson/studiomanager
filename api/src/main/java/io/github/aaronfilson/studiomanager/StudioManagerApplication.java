package io.github.aaronfilson.studiomanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class StudioManagerApplication {

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(StudioManagerApplication.class);
    // Records each startup step, so StartupReport can log the slowest ones (plan 23)
    BufferingApplicationStartup startup = new BufferingApplicationStartup(StartupReport.CAPACITY);
    application.setApplicationStartup(startup);
    application.addListeners(new StartupReport(startup));
    ConfigurableApplicationContext context = application.run(args);
    // The migrate and sweep profiles have no web server: Flyway, or every sweep (SweepJob), ran
    // during startup, so the job is done. The exit code says whether it succeeded.
    if (context.getEnvironment().matchesProfiles("migrate | sweep")) {
      System.exit(SpringApplication.exit(context));
    }
  }
}
