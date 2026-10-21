package io.github.aaronfilson.studiomanager.platform;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retries failed event publications, such as an email the provider refused, and clears out old
 * completed ones.
 *
 * <p>A failure waits at least {@link #MIN_AGE} before its next attempt, and gives up after
 * {@link #MAX_ATTEMPTS}, staying in {@code event_publication} as {@code FAILED} for someone to look
 * at. Completed publications are kept for {@link #KEEP_COMPLETED}, so "did that email go out?"
 * has an answer.
 */
@Component
@ConditionalOnBooleanProperty(name = "studiomanager.outbox.retry", matchIfMissing = true)
class OutboxRetry {

    static final Duration MIN_AGE = Duration.ofMinutes(5);
    static final int MAX_ATTEMPTS = 8;
    static final Duration KEEP_COMPLETED = Duration.ofDays(30);

    private final FailedEventPublications failed;
    private final CompletedEventPublications completed;

    OutboxRetry(FailedEventPublications failed, CompletedEventPublications completed) {
        this.failed = failed;
        this.completed = completed;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT5M")
    void retryFailed() {
        failed.resubmit(ResubmissionOptions.defaults()
                .withMinAge(MIN_AGE)
                .withFilter(publication -> publication.getCompletionAttempts() < MAX_ATTEMPTS));
    }

    @Scheduled(initialDelayString = "PT10M", fixedDelayString = "PT24H")
    void purgeCompleted() {
        completed.deletePublicationsOlderThan(KEEP_COMPLETED);
    }

}
