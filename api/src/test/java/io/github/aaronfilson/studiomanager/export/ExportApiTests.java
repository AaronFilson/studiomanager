package io.github.aaronfilson.studiomanager.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.jayway.jsonpath.JsonPath;
import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** A studio's owner downloads all of its data, and nothing of any other studio's. */
@IntegrationTest
class ExportApiTests {

    @Autowired
    MockMvcTester mvc;

    private final TestUser alice = TestUser.named("Alice");
    private final TestUser bob = TestUser.named("Bob");
    private final TestUser maker = TestUser.named("Mo");

    @Test
    void theOwnerGetsEveryTableOfTheirStudioAndNoOtherStudiosRows() throws Exception {
        UUID alices = createStudio(alice, "Alice's Clay");
        UUID bobs = createStudio(bob, "Bob's Kiln");
        String code = read(post(alice, "/api/v1/studios/" + alices + "/invites", "{\"role\": \"MEMBER\"}"), "$.code");
        assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(maker.token())).hasStatusOk();
        // A description that a spreadsheet would run as a formula
        post(alice, "/api/v1/studios/" + alices + "/pieces",
                "{\"size\": {}, \"description\": \"=HYPERLINK(\\\"http://evil.test\\\")\"}");
        post(bob, "/api/v1/studios/" + bobs + "/pieces", "{\"size\": {}, \"description\": \"Bob's bowl\"}");
        post(bob, "/api/v1/studios/" + bobs + "/invites", "{\"role\": \"MEMBER\"}");

        MvcTestResult result = mvc.get().uri("/api/v1/studios/{id}/export", alices).with(alice.token()).exchange();
        assertThat(result).hasStatusOk().hasContentType("application/zip");
        assertThat(result.getResponse().getHeader("Content-Disposition")).startsWith("attachment; filename=\"studio-export-");
        Map<String, String> files = unzip(result.getResponse().getContentAsByteArray());

        assertThat(files).containsKeys("README.txt", "studio.csv", "members.csv", "piece.csv", "piece_event.csv",
                "studio_membership.csv", "studio_invite.csv", "fee_schedule.csv", "asset.csv", "journal_line.csv");
        // Nothing of Bob's studio, in any file
        files.forEach((name, content) -> assertThat(content).as(name).doesNotContain(bobs.toString())
                .doesNotContain("Bob's"));
        assertThat(files.get("studio.csv")).contains("Alice's Clay");
        assertThat(files.get("members.csv").lines().toList())
                .first().isEqualTo("account_id,name,email,role,joined_at");
        assertThat(files.get("members.csv")).contains(alice.email()).contains(maker.email()).contains("OWNER");
        // The formula is defused, and the piece is there
        assertThat(files.get("piece.csv")).contains("'=HYPERLINK(").contains(alices.toString());
        // An invite's code hash stays behind
        assertThat(files.get("studio_invite.csv").lines().findFirst().orElseThrow()).doesNotContain("code_hash")
                .contains("expires_at");
        // A table with no rows still has its header
        assertThat(files.get("payout.csv").lines().toList()).hasSize(1).first().asString().contains("studio_id");
    }

    @Test
    void onlyTheOwnerMayExport() throws Exception {
        UUID studio = createStudio(alice, "Alice's Clay");
        String code = read(post(alice, "/api/v1/studios/" + studio + "/invites", "{\"role\": \"STAFF\"}"), "$.code");
        assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(maker.token())).hasStatusOk();

        assertThat(mvc.get().uri("/api/v1/studios/{id}/export", studio).with(maker.token()))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/api/v1/studios/{id}/export", studio).with(bob.token()))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    private static Map<String, String> unzip(byte[] zip) throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                files.put(entry.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return files;
    }

    private UUID createStudio(TestUser owner, String name) throws Exception {
        return UUID.fromString(read(post(owner, "/api/v1/studios", """
                {"name": "%s", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                """.formatted(name)), "$.id"));
    }

    private MvcTestResult post(TestUser user, String uri, String json) {
        MvcTestResult result = mvc.post().uri(uri).with(user.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json).exchange();
        assertThat(result.getResponse().getStatus()).as(uri).isIn(List.of(200, 201));
        return result;
    }

    private static String read(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

}
