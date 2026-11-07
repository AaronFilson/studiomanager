package io.github.aaronfilson.studiomanager.maintenance;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.StatementCounts;
import io.github.aaronfilson.studiomanager.TestUser;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Instant;
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
 * Maintenance on Hibernate (ADR 21): lists at a fixed number of statements, the client's version
 * checked, and the tenant applied by Hibernate as well as by row-level security.
 */
@IntegrationTest
class MaintenancePersistenceTests {

  @Autowired MockMvcTester mvc;
  @Autowired StatementCounts statements;
  @Autowired MaintenanceService maintenance;
  @Autowired MaintenanceRecords records;
  @Autowired ProblemReportService problems;
  @Autowired MaintainedAssetRepository assetRepository;
  @Autowired TransactionOperations transactions;

  private final TestUser owner = TestUser.named("Ines");
  private UUID studioId;
  private UUID ownerAccountId;

  @BeforeEach
  void studio() throws Exception {
    studioId = UUID.fromString(read(createStudio(owner, "Fixed Upkeep"), "$.id"));
    ownerAccountId = accountId(owner);
  }

  @Test
  void theAssetListIsTheSameNumberOfQueriesForOneAssetOrMany() throws Exception {
    taskOn(asset("WHEEL", "Wheel 1"), "Clean the splash pan");
    long forOne = countAssets();

    taskOn(asset("WHEEL", "Wheel 2"), "Clean the splash pan");
    taskOn(asset("PUG_MILL", "Peter"), "Clean the auger");
    kiln("Skutt");
    long forMany = countAssets();

    assertThat(TenantContext.callAs(inStudio(), () -> maintenance.assets(Instant.now())))
        .hasSize(4);
    assertThat(forMany).isEqualTo(forOne);
  }

  @Test
  void anAssetsHistoryIsTheSameNumberOfQueriesForOneRecordOrMany() throws Exception {
    String trap = asset("CLAY_TRAP", "Sink trap");
    String task = taskOn(trap, "Empty the trap");
    complete(task);
    long forOne = countHistory(trap);

    complete(task);
    repair(trap, "New baffle");
    long forThree = countHistory(trap);

    assertThat(
            TenantContext.callAs(inStudio(), () -> records.records(UUID.fromString(trap)))
                .items())
        .extracting(MaintenanceModel.WorkRecord::taskTitle)
        .containsExactly(null, "Empty the trap", "Empty the trap");
    assertThat(forThree).isEqualTo(forOne);
  }

  @Test
  void problemReportsAreTheSameNumberOfQueriesForOneOrMany() throws Exception {
    String wheel = asset("WHEEL", "Wheel 3");
    report(wheel, "Grinds");
    long forOne = countReports();

    report(wheel, "Wobbles");
    report(null, "The sink is blocked");
    long forThree = countReports();

    assertThat(forThree).isEqualTo(forOne);
  }

  @Test
  void aStaleVersionIsAConflictAndTheCurrentOneSaves() throws Exception {
    String task = taskOn(asset("SPRAY_BOOTH", "Booth"), "Change the filter");
    String change =
        """
        {"task": {"title": "Change both filters", "everyDays": 30}, "expectedVersion": %d}
        """;
    assertThat(put("/maintenance/tasks/" + task, change.formatted(0)))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.version")
        .isEqualTo(1);
    assertThat(put("/maintenance/tasks/" + task, change.formatted(0)))
        .hasStatus(HttpStatus.CONFLICT);
  }

  @Test
  void aTakenNameIsAnInputErrorNotAServerError() throws Exception {
    asset("SLAB_ROLLER", "Twin");
    assertThat(post("/maintenance/assets", "{\"kind\": \"WHEEL\", \"name\": \"twin\"}"))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void hibernateSeesOnlyTheBoundStudiosAssetsAndNoneWithNoStudioBound() throws Exception {
    UUID ours = UUID.fromString(asset("VENTILATION", "Fan"));
    TestUser stranger = TestUser.named("Uma");
    UUID theirStudio = UUID.fromString(read(createStudio(stranger, "Elsewhere Upkeep"), "$.id"));
    TenantContext theirs = new TenantContext(accountId(stranger), theirStudio, StudioRole.OWNER);

    assertThat(TenantContext.callAs(theirs, this::readAssetIds)).doesNotContain(ours);
    assertThat(TenantContext.callAs(theirs, () -> findAsset(ours))).isFalse();
    assertThat(TenantContext.callAs(TenantContext.forAccount(ownerAccountId), this::readAssetIds))
        .isEmpty();
  }

  private List<UUID> readAssetIds() {
    return transactions.execute(
        status -> assetRepository.findTracked().stream().map(MaintainedAsset::id).toList());
  }

  private boolean findAsset(UUID assetId) {
    return Boolean.TRUE.equals(
        transactions.execute(status -> assetRepository.findById(assetId).isPresent()));
  }

  private long countAssets() throws Exception {
    return statements
        .of(() -> TenantContext.callAs(inStudio(), () -> maintenance.assets(Instant.now())))
        .statements();
  }

  private long countHistory(String assetId) throws Exception {
    return statements
        .of(() -> TenantContext.callAs(inStudio(), () -> records.records(UUID.fromString(assetId))))
        .statements();
  }

  private long countReports() throws Exception {
    return statements
        .of(() -> TenantContext.callAs(inStudio(), () -> problems.reports(false)))
        .statements();
  }

  private TenantContext inStudio() {
    return new TenantContext(ownerAccountId, studioId, StudioRole.OWNER);
  }

  private String asset(String kind, String name) throws Exception {
    return read(
        created(
            post(
                "/maintenance/assets",
                "{\"kind\": \"%s\", \"name\": \"%s\"}".formatted(kind, name))),
        "$.id");
  }

  private void kiln(String name) {
    created(post("/kilns", "{\"name\": \"%s\", \"kind\": \"ELECTRIC\"}".formatted(name)));
  }

  private String taskOn(String assetId, String title) throws Exception {
    return read(
        created(
            post(
                "/maintenance/assets/" + assetId + "/tasks",
                "{\"title\": \"%s\", \"everyDays\": 7}".formatted(title))),
        "$.id");
  }

  private void complete(String taskId) {
    assertThat(post("/maintenance/tasks/" + taskId + "/completions", "{}")).hasStatusOk();
  }

  private void repair(String assetId, String note) {
    created(
        post(
            "/maintenance/assets/" + assetId + "/records",
            "{\"note\": \"%s\"}".formatted(note)));
  }

  private void report(String assetId, String description) {
    String json =
        assetId == null
            ? "{\"description\": \"%s\"}".formatted(description)
            : "{\"assetId\": \"%s\", \"description\": \"%s\"}".formatted(assetId, description);
    created(post("/maintenance/problem-reports", json));
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

  private MvcTestResult put(String suffix, String json) {
    return mvc.put()
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
