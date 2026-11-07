package io.github.aaronfilson.studiomanager.notify;

import io.github.aaronfilson.studiomanager.kiln.FiringCompleted;
import io.github.aaronfilson.studiomanager.kiln.FiringKind;
import io.github.aaronfilson.studiomanager.maintenance.DueStatus;
import io.github.aaronfilson.studiomanager.maintenance.MaintenanceDue;
import io.github.aaronfilson.studiomanager.maintenance.ProblemReported;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.schedule.ShiftCovered;
import io.github.aaronfilson.studiomanager.schedule.ShiftOffered;
import io.github.aaronfilson.studiomanager.studio.MemberView;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import io.github.aaronfilson.studiomanager.supplies.SupplyRanOut;
import io.github.aaronfilson.studiomanager.webhook.WebhookEndpointDisabled;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Turns what happens in other modules into notices, in the transaction that caused them.
 *
 * <p>The research behind this: the most specific complaint about an existing tracker was "ready"
 * emails going out before all of a member's pieces were out. So a firing produces exactly one
 * notice per maker, listing every piece of theirs it finished.
 */
@Component
class NotificationListeners {

  private final Notices notices;
  private final StudioService studios;
  private final StudioMembers members;
  private final Clock clock;

  NotificationListeners(
      Notices notices, StudioService studios, StudioMembers members, Clock clock) {
    this.notices = notices;
    this.studios = studios;
    this.members = members;
    this.clock = clock;
  }

  /** One notice per maker for the whole firing, listing each of their pieces it finished. */
  @EventListener
  void onFiringCompleted(FiringCompleted firing) {
    // "4 Nov 2026": unambiguous in every country the product serves
    DateTimeFormatter dateFormat = dateFormat();
    Map<UUID, List<FiringCompleted.FiredPiece>> byMaker =
        firing.pieces().stream()
            .collect(Collectors.groupingBy(FiringCompleted.FiredPiece::makerAccountId));
    byMaker.forEach((maker, theirs) -> {
      List<FiringCompleted.FiredPiece> sorted =
          theirs.stream()
              .sorted(Comparator.comparingInt(FiringCompleted.FiredPiece::number))
              .toList();
      int count = sorted.size();
      String noun = count == 1 ? "piece is" : count + " pieces are";
      String title = firedTitle(firing.kind(), noun);
      StringBuilder body = new StringBuilder();
      for (FiringCompleted.FiredPiece piece : sorted) {
        body.append("Piece ").append(piece.number());
        String description = piece.description();
        if (description != null) {
          body.append(": ").append(description);
        }
        body.append('\n');
      }
      sorted.stream()
          .map(FiringCompleted.FiredPiece::holdUntil)
          .filter(Objects::nonNull)
          .findFirst()
          .ifPresent(hold ->
              body.append("Please collect by ")
                  .append(dateFormat.format(hold.minusSeconds(1)))
                  .append('.'));
      notices.send(
          List.of(maker), NotificationView.Kind.PIECES_READY, title, body.toString().strip());
    });
  }

  private static String firedTitle(FiringKind kind, String noun) {
    return switch (kind) {
      case GLAZE -> "Your %s ready to collect".formatted(noun);
      case BISQUE -> "Your %s bisque fired and ready to glaze".formatted(noun);
    };
  }

  /** One notice to each person the task falls to, with what to do and by when. */
  @EventListener
  void onMaintenanceDue(MaintenanceDue due) {
    DateTimeFormatter dateFormat = dateFormat();
    String title = (due.status() == DueStatus.DUE ? "Due: " : "Due soon: ") + due.taskTitle();
    StringBuilder body =
        new StringBuilder(due.assetName()).append(": ").append(due.taskTitle()).append('\n');
    Instant dueAt = due.dueAt();
    if (dueAt != null) {
      body.append(
              due.status() == DueStatus.DUE && !dueAt.isAfter(clock.instant())
                  ? "It was due on "
                  : "Due by ")
          .append(dateFormat.format(dueAt))
          .append(".\n");
    }
    Integer firingsLeft = due.firingsLeft();
    if (firingsLeft != null) {
      int left = firingsLeft;
      body.append(
              left <= 0
                  ? "It's due by firing count."
                  : left == 1 ? "1 firing left." : left + " firings left.")
          .append('\n');
    }
    if (!due.checklist().isEmpty()) {
      body.append('\n');
      due.checklist().forEach(line -> body.append("- ").append(line).append('\n'));
    }
    body.append("\nMark it done on the Maintenance page when it's finished.");
    notices.send(
        due.recipients(),
        NotificationView.Kind.MAINTENANCE_DUE,
        clip(title, 120),
        clip(body.toString(), 2000));
  }

  /**
   * "B-Mix 5 is out": tells every member what to use instead, so nobody finds the empty shelf
   * first. Sent as a studio notice, the same as one staff would write.
   */
  @EventListener
  void onSupplyRanOut(SupplyRanOut ranOut) {
    List<UUID> everyone = members.list().stream().map(MemberView::accountId).toList();
    notices.send(
        everyone,
        NotificationView.Kind.STUDIO_NOTICE,
        clip(ranOut.itemName() + " is out", 120),
        clip(ranOut.substitute(), 2000));
  }

  /** "Mona needs cover: Monitor, Tue 3 Nov 6:00 pm", to everyone who could take it. */
  @EventListener
  void onShiftOffered(ShiftOffered offer) {
    String when = shiftTime(offer.startsAt(), offer.endsAt());
    String holder = offer.holderName();
    notices.send(
        offer.recipients(),
        NotificationView.Kind.SHIFT_COVER,
        clip(
            "%s needs cover: %s".formatted(holder == null ? "Someone" : holder, offer.title()),
            120),
        "%s, %s.\n\nTake it on the Roster page if you can.".formatted(offer.title(), when));
  }

  /** Tells whoever asked for cover that they have it. */
  @EventListener
  void onShiftCovered(ShiftCovered covered) {
    String coveredBy = covered.coveredByName();
    String who = coveredBy == null ? "Someone" : coveredBy;
    notices.send(
        List.of(covered.previousHolder()),
        NotificationView.Kind.SHIFT_COVER,
        clip("%s is covering your shift".formatted(who), 120),
        "%s is covering %s on %s. It's off your roster."
            .formatted(who, covered.title(), shiftTime(covered.startsAt(), null)));
  }

  /** Tells the staff and technicians what a member noticed, in the member's words. */
  @EventListener
  void onProblemReported(ProblemReported report) {
    String assetName = report.assetName();
    String reportedBy = report.reportedByName();
    String what = assetName == null ? "a problem" : "a problem with " + assetName;
    String title = "Reported: " + (assetName == null ? "a problem" : assetName);
    String body =
        "%s reported %s:\n\n%s\n\nSee it on the Maintenance page, and resolve it there once it's dealt with."
            .formatted(reportedBy == null ? "A member" : reportedBy, what, report.description());
    notices.send(
        report.recipients(),
        NotificationView.Kind.PROBLEM_REPORTED,
        clip(title, 120),
        clip(body, 2000));
  }

  /** Tells staff that an integration stopped, so a broken Zap isn't found weeks later. */
  @EventListener
  void onWebhookEndpointDisabled(WebhookEndpointDisabled disabled) {
    List<UUID> staff =
        members.list().stream()
            .filter(member -> member.role().includes(StudioRole.STAFF))
            .map(MemberView::accountId)
            .toList();
    String body =
        """
        Webhooks to %s have been turned off.

        %s

        Fix the receiver, then turn the endpoint back on from the Webhooks page under Settings. \
        Events from while it was off aren't sent again, but you can resend any of them from its log.\
        """
            .formatted(disabled.url(), disabled.reason());
    notices.send(
        staff,
        NotificationView.Kind.WEBHOOK_DISABLED,
        "A webhook was turned off",
        clip(body, 2000));
  }

  /** "Tue 3 Nov 2026, 6:00 pm–9:00 pm" in the studio's time zone. */
  private String shiftTime(Instant start, @Nullable Instant end) {
    ZoneId zone = studios.current().timeZone();
    DateTimeFormatter day =
        DateTimeFormatter.ofPattern("EEE d MMM yyyy, h:mm a", Locale.ENGLISH).withZone(zone);
    DateTimeFormatter time = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH).withZone(zone);
    return end == null ? day.format(start) : day.format(start) + "–" + time.format(end);
  }

  /** "4 Nov 2026" in the studio's time zone. */
  private DateTimeFormatter dateFormat() {
    return DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
        .withZone(studios.current().timeZone());
  }

  private static String clip(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max - 1) + "…";
  }
}
