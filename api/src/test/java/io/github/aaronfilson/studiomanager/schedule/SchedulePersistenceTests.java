package io.github.aaronfilson.studiomanager.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.StatementCounts;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Schedule on Hibernate (ADR 21): the calendar, the roster and the hours ledger at a fixed number
 * of statements, the client's version checked, and the tenant applied by Hibernate as well as by
 * row-level security.
 */
@IntegrationTest
class SchedulePersistenceTests {

  @Autowired MockMvcTester mvc;
  @Autowired StatementCounts statements;
  @Autowired ScheduleService schedule;
  @Autowired BookableResourceRepository resourceRepository;
  @Autowired TransactionOperations transactions;

  private final TestUser owner = TestUser.named("Lou");
  private final Instant tomorrow =
      Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
  private UUID studioId;
  private UUID ownerAccountId;

  @BeforeEach
  void studio() throws Exception {
    studioId = UUID.fromString(read(createStudio(owner, "Fixed Calendar"), "$.id"));
    ownerAccountId = accountId(owner);
  }

  @Test
  void theCalendarIsTheSameNumberOfQueriesForOneBookingOrMany() throws Exception {
    String wheels = resource("Wheel room", 8);
    book(wheels, "CLASS", tomorrow);
    long forOne = countBookings();

    book(wheels, "RESERVATION", tomorrow.plus(1, ChronoUnit.DAYS));
    book(resource("Slab table", 1), "RENTAL", tomorrow.plus(2, ChronoUnit.DAYS));
    long forThree = countBookings();

    assertThat(
            TenantContext.callAs(inStudio(), () -> schedule.bookings(tomorrow, weekLater()))
                .items())
        .extracting(ScheduleModel.Booking::resourceName)
        .containsExactly("Wheel room", "Wheel room", "Slab table");
    assertThat(forThree).isEqualTo(forOne);
  }

  @Test
  void theRosterIsTheSameNumberOfQueriesForOneShiftOrMany() throws Exception {
    shifts("Monitor", 1);
    long forOne = countShifts();

    String first = shifts("Glaze mixing", 2);
    assign(first, ownerAccountId, 0);
    long forThree = countShifts();

    assertThat(forThree).isEqualTo(forOne);
  }

  @Test
  void theHoursLedgerIsTheSameNumberOfQueriesForOneEntryOrMany() throws Exception {
    adjust(30, "Kiln unloading");
    long forOne = countHours();

    adjust(45, "Shelf scrubbing");
    adjust(-15, "Left early");
    long forThree = countHours();

    assertThat(TenantContext.callAs(inStudio(), () -> schedule.hours(ownerAccountId)))
        .extracting(ScheduleModel.HoursLedger::totalMinutes)
        .isEqualTo(60);
    assertThat(forThree).isEqualTo(forOne);
  }

  @Test
  void aStaleVersionIsAConflictAndTheCurrentOneSaves() throws Exception {
    String shift = shifts("Front desk", 1);
    assertThat(assign(shift, ownerAccountId, 0))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.version")
        .isEqualTo(1);
    assertThat(assign(shift, null, 0)).hasStatus(HttpStatus.CONFLICT);
  }

  @Test
  void aTakenNameIsAnInputErrorNotAServerError() throws Exception {
    resource("Glaze room", 2);
    assertThat(
            post(
                "/resources",
                "{\"name\": \"Glaze room\", \"kind\": \"GLAZE_ROOM\", \"capacity\": 2}"))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void hibernateSeesOnlyTheBoundStudiosResourcesAndNoneWithNoStudioBound() throws Exception {
    UUID ours = UUID.fromString(resource("Big kiln", 1));
    TestUser stranger = TestUser.named("Vic");
    UUID theirStudio = UUID.fromString(read(createStudio(stranger, "Elsewhere Calendar"), "$.id"));
    TenantContext theirs = new TenantContext(accountId(stranger), theirStudio, StudioRole.OWNER);

    assertThat(TenantContext.callAs(theirs, this::readResourceIds)).doesNotContain(ours);
    assertThat(
            TenantContext.callAs(TenantContext.forAccount(ownerAccountId), this::readResourceIds))
        .isEmpty();
  }

  private List<UUID> readResourceIds() {
    return transactions.execute(
        status -> resourceRepository.findAllByName().stream().map(BookableResource::id).toList());
  }

  private long countBookings() throws Exception {
    return statements
        .of(() -> TenantContext.callAs(inStudio(), () -> schedule.bookings(tomorrow, weekLater())))
        .statements();
  }

  private long countShifts() throws Exception {
    return statements
        .of(() -> TenantContext.callAs(inStudio(), () -> schedule.shifts(tomorrow, weekLater())))
        .statements();
  }

  private long countHours() throws Exception {
    return statements
        .of(() -> TenantContext.callAs(inStudio(), () -> schedule.hours(ownerAccountId)))
        .statements();
  }

  private Instant weekLater() {
    return tomorrow.plus(7, ChronoUnit.DAYS);
  }

  private TenantContext inStudio() {
    return new TenantContext(ownerAccountId, studioId, StudioRole.OWNER);
  }

  private String resource(String name, int capacity) throws Exception {
    return read(
        created(
            post(
                "/resources",
                "{\"name\": \"%s\", \"kind\": \"OTHER\", \"capacity\": %d}"
                    .formatted(name, capacity))),
        "$.id");
  }

  private void book(String resourceId, String kind, Instant startsAt) {
    created(
        post(
            "/bookings",
            """
            {"resourceId": "%s", "kind": "%s", "startsAt": "%s", "endsAt": "%s"}
            """
                .formatted(resourceId, kind, startsAt, startsAt.plus(2, ChronoUnit.HOURS))));
  }

  /** Creates weekly shifts from tomorrow, and returns the first one's id. */
  private String shifts(String title, int weeks) throws Exception {
    return read(
        created(
            post(
                "/shifts",
                """
                {"title": "%s", "firstStartsAt": "%s", "minutes": 120, "weeks": %d}
                """
                    .formatted(title, tomorrow.plus(1, ChronoUnit.HOURS), weeks))),
        "$[0].id");
  }

  private MvcTestResult assign(String shiftId, UUID accountId, long version) {
    String person = accountId == null ? "null" : "\"%s\"".formatted(accountId);
    return mvc.put()
        .uri(path("/shifts/" + shiftId + "/assignment"))
        .with(owner.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"accountId\": %s, \"expectedVersion\": %d}".formatted(person, version))
        .exchange();
  }

  private void adjust(int minutes, String note) {
    assertThat(
            post(
                "/hours/" + ownerAccountId,
                "{\"minutes\": %d, \"note\": \"%s\"}".formatted(minutes, note)))
        .hasStatusOk();
  }

  private UUID accountId(TestUser user) throws Exception {
    return UUID.fromString(
        read(mvc.get().uri("/api/v1/me").with(user.token()).exchange(), "$.account.id"));
  }

  private MvcTestResult createStudio(TestUser user, String name) {
    return mvc.post()
        .uri("/api/v1/studios")
        .with(user.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            """
            {"name": "%s", "unitSystem": "METRIC", "timeZone": "Europe/London", "country": "GB"}
            """
                .formatted(name))
        .exchange();
  }

  private static MvcTestResult created(MvcTestResult result) {
    assertThat(result).hasStatus(HttpStatus.CREATED);
    return result;
  }

  private MvcTestResult post(String suffix, String json) {
    return mvc.post()
        .uri(path(suffix))
        .with(owner.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .exchange();
  }

  private String path(String suffix) {
    return "/api/v1/studios/" + studioId + suffix;
  }

  private static String read(MvcTestResult result, String jsonPath) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
  }
}
