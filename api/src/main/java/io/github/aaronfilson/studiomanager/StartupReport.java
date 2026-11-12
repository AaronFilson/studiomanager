package io.github.aaronfilson.studiomanager;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.metrics.buffering.StartupTimeline.TimelineEvent;
import org.springframework.context.ApplicationListener;

/**
 * Logs where a start spent its time (plan 23): the JVM's uptime when the app is ready, the CPUs and
 * heap the replica gave it, and the slowest startup steps Spring recorded. On Container Apps a cold
 * start is a request waiting, so this line in the logs is the first thing to read.
 */
final class StartupReport implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger log = LoggerFactory.getLogger(StartupReport.class);

  /** Enough steps for a full start of this app; later ones are dropped, not kept in memory. */
  static final int CAPACITY = 20_000;

  private static final int SLOWEST = 15;

  private final BufferingApplicationStartup startup;

  StartupReport(BufferingApplicationStartup startup) {
    this.startup = startup;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    Runtime runtime = Runtime.getRuntime();
    long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
    Duration spring = event.getTimeTaken();
    String slowest =
        startup.drainBufferedTimeline().getEvents().stream()
            .sorted(Comparator.comparing(TimelineEvent::getDuration).reversed())
            .limit(SLOWEST)
            .map(StartupReport::describe)
            .collect(Collectors.joining("; "));
    log.info(
        "Started: JVM uptime {} ms, Spring {} ms, {} CPUs, max heap {} MiB, AOT cache {}."
            + " Slowest steps: {}",
        uptimeMillis,
        spring == null ? -1 : spring.toMillis(),
        runtime.availableProcessors(),
        runtime.maxMemory() / (1024 * 1024),
        aotCacheFlag(),
        slowest);
  }

  private static String describe(TimelineEvent event) {
    StringBuilder text = new StringBuilder(event.getStartupStep().getName());
    for (var tag : event.getStartupStep().getTags()) {
      if (tag.getKey().equals("beanName")) {
        text.append(' ').append(tag.getValue());
      }
    }
    return text.append(' ').append(event.getDuration().toMillis()).append(" ms").toString();
  }

  /**
   * Whether the JVM was asked to use the AOT cache (ADR 8). It says nothing about whether it
   * loaded.
   */
  private static String aotCacheFlag() {
    return ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
        .filter(argument -> argument.startsWith("-XX:AOTCache="))
        .findFirst()
        .orElse("not set");
  }
}
