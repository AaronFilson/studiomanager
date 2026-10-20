package io.github.aaronfilson.studiomanager.piece;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The M1 loop through the API: fees, invites, intake, moves, the hold, and who sees what. */
@IntegrationTest
class PieceApiTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    UnclaimedPieceSweep sweep;

    private final TestUser owner = TestUser.named("Olive");
    private final TestUser maker = TestUser.named("Mo");
    private final TestUser otherMaker = TestUser.named("Nia");
    private UUID studioId;

    @BeforeEach
    void studioWithTwoMembers() throws Exception {
        studioId = UUID.fromString(read(post(owner, "/api/v1/studios", """
                {"name": "Riverside Clay", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                """), "$.id"));
        join(maker);
        join(otherMaker);
    }

    @Test
    void pricesAtIntakeAndKeepsThatPriceWhenFeesChange() throws Exception {
        saveFees("""
                {"ruleType": "PER_VOLUME", "rate": "0.05", "unit": "CUBIC_INCH", "rounding": "WHOLE_UNIT", "minimum": "2.00"}
                """);

        MvcTestResult intake = post(maker, studioPath("/pieces"), """
                {"description": "Lidded jar", "makerMark": "MO",
                 "size": {"dimensions": {"length": "11.2", "width": "9.5", "height": "7.9", "unit": "INCH"}}}
                """);
        assertThat(intake).hasStatus(HttpStatus.CREATED).bodyJson()
                .hasPathSatisfying("$.number", n -> assertThat(n).isEqualTo(1))
                .hasPathSatisfying("$.stage", s -> assertThat(s).isEqualTo("RECEIVED"))
                .hasPathSatisfying("$.fee.amount.amount", a -> assertThat(a).isEqualTo("48.00"))
                .hasPathSatisfying("$.fee.amount.currency", c -> assertThat(c).isEqualTo("USD"))
                .hasPathSatisfying("$.fee.scheduleVersion", v -> assertThat(v).isEqualTo(1))
                .hasPathSatisfying("$.fee.breakdown", b -> assertThat(b).isEqualTo("12 × 10 × 8 in → 960 in³ × 0.05 = 48.00"));
        String pieceId = read(intake, "$.id");

        saveFees("""
                {"ruleType": "FLAT", "rate": "10", "rounding": "NONE", "minimum": "0"}
                """);

        assertThat(mvc.get().uri(studioPath("/pieces/" + pieceId)).with(maker.token()))
                .hasStatusOk().bodyJson().extractingPath("$.fee.amount.amount").isEqualTo("48.00");
        assertThat(mvc.get().uri(studioPath("/fee-schedule")).with(maker.token()))
                .hasStatusOk().bodyJson().extractingPath("$.version").isEqualTo(2);
    }

    @Test
    void membersSeeOnlyTheirOwnPiecesAndStaffSeeAll() throws Exception {
        String mine = read(intake(maker), "$.id");
        read(intake(otherMaker), "$.id");

        assertThat(mvc.get().uri(studioPath("/pieces")).with(maker.token()))
                .hasStatusOk().bodyJson().extractingPath("$[*].id").asArray().containsExactly(mine);
        assertThat(mvc.get().uri(studioPath("/pieces")).with(owner.token()))
                .hasStatusOk().bodyJson().extractingPath("$.length()").isEqualTo(2);
        // Someone else's piece is a 404, not a 403, so its existence isn't revealed
        String theirs = read(intake(otherMaker), "$.id");
        assertThat(mvc.get().uri(studioPath("/pieces/" + theirs)).with(maker.token()))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void staffMoveAPieceToReadyWhichStartsTheHoldAndTheSweepMarksItUnclaimed() throws Exception {
        String pieceId = read(intake(maker), "$.id");

        MvcTestResult bisqued = move(owner, pieceId, "BISQUE_FIRED", 0);
        assertThat(bisqued).hasStatusOk();
        assertThat(move(owner, pieceId, "READY", 1)).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.holdUntil", h -> assertThat(h).isNotNull())
                .hasPathSatisfying("$.nextStages", n -> assertThat(n).asArray().containsExactlyInAnyOrder("PICKED_UP", "UNCLAIMED"));

        // Long after the 30-day default hold
        assertThat(sweep.runAt(Instant.now().plusSeconds(60L * 60 * 24 * 40))).isGreaterThanOrEqualTo(1);

        assertThat(mvc.get().uri(studioPath("/pieces/" + pieceId)).with(maker.token()))
                .hasStatusOk().bodyJson()
                .hasPathSatisfying("$.stage", s -> assertThat(s).isEqualTo("UNCLAIMED"))
                .hasPathSatisfying("$.timeline[*].to", t -> assertThat(t).asArray()
                        .containsExactly("RECEIVED", "BISQUE_FIRED", "READY", "UNCLAIMED"))
                .hasPathSatisfying("$.timeline[3].note", n -> assertThat(n).isEqualTo("Hold period ended"));
    }

    @Test
    void refusesImpossibleMovesStaleVersionsAndMembersMoving() throws Exception {
        String pieceId = read(intake(maker), "$.id");

        assertThat(move(owner, pieceId, "PICKED_UP", 0)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(move(maker, pieceId, "BISQUE_FIRED", 0)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(move(owner, pieceId, "BISQUE_FIRED", 0)).hasStatusOk();
        assertThat(move(owner, pieceId, "GLAZED", 0)).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void onlyStaffTakeInPiecesForSomeoneElse() throws Exception {
        String makerId = accountId(maker);

        assertThat(post(otherMaker, studioPath("/pieces"), """
                {"makerAccountId": "%s", "size": {}}
                """.formatted(makerId))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(owner, studioPath("/pieces"), """
                {"makerAccountId": "%s", "size": {}}
                """.formatted(makerId))).hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.makerName").isEqualTo("Mo");
    }

    @Test
    void anInviteWorksOnceAndStaffCantMintOwners() throws Exception {
        String code = read(post(owner, studioPath("/invites"), """
                {"role": "STAFF"}
                """), "$.code");
        TestUser staff = TestUser.named("Sam");
        assertThat(accept(staff, code)).hasStatusOk().bodyJson().extractingPath("$.role").isEqualTo("STAFF");
        assertThat(accept(TestUser.named("Late"), code)).hasStatus(HttpStatus.NOT_FOUND);

        assertThat(post(staff, studioPath("/invites"), """
                {"role": "OWNER"}
                """)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(maker, studioPath("/invites"), """
                {"role": "MEMBER"}
                """)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void holdDaysAreAStudioSetting() throws Exception {
        assertThat(mvc.put().uri(studioPath("/piece-policy")).with(owner.token())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"holdDays": 14}
                        """))
                .hasStatusOk().bodyJson().extractingPath("$.holdDays").isEqualTo(14);
        assertThat(mvc.get().uri(studioPath("/piece-policy")).with(maker.token()))
                .hasStatusOk().bodyJson().extractingPath("$.holdDays").isEqualTo(14);
    }

    private MvcTestResult accept(TestUser user, String code) {
        return mvc.post().uri("/api/v1/invites/{code}/accept", code).with(user.token()).exchange();
    }

    private void join(TestUser user) throws Exception {
        String code = read(post(owner, studioPath("/invites"), """
                {"role": "MEMBER"}
                """), "$.code");
        assertThat(accept(user, code)).hasStatusOk();
    }

    private MvcTestResult intake(TestUser user) {
        return post(user, studioPath("/pieces"), """
                {"size": {}}
                """);
    }

    private MvcTestResult move(TestUser user, String pieceId, String to, long expectedVersion) {
        return post(user, studioPath("/pieces/" + pieceId + "/moves"), """
                {"to": "%s", "expectedVersion": %d}
                """.formatted(to, expectedVersion));
    }

    private void saveFees(String json) {
        assertThat(mvc.put().uri(studioPath("/fee-schedule")).with(owner.token())
                .contentType(MediaType.APPLICATION_JSON).content(json)).hasStatusOk();
    }

    private String accountId(TestUser user) throws Exception {
        return read(mvc.get().uri("/api/v1/me").with(user.token()).exchange(), "$.account.id");
    }

    private MvcTestResult post(TestUser user, String path, String json) {
        return mvc.post().uri(path).with(user.token()).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private String studioPath(String suffix) {
        return "/api/v1/studios/" + studioId + suffix;
    }

    private static String read(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path).toString();
    }

}
