package io.github.aaronfilson.studiomanager.maintenance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
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

/** Photos on maintenance records, through the API and a real Azurite, as for pieces. */
@IntegrationTest
class RecordPhotoApiTests {

    /** Enough of a JPEG for the API's check, which reads only the first bytes. */
    private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0 };

    private final HttpClient http = HttpClient.newHttpClient();

    @Autowired
    MockMvcTester mvc;

    @Autowired
    RecordPhotoUploadSweep sweep;

    private final TestUser owner = TestUser.named("Olive");
    private final TestUser maker = TestUser.named("Mo");
    private UUID studioId;
    private String recordId;

    @BeforeEach
    void aRepair() throws Exception {
        studioId = UUID.fromString(read(post(owner, "/api/v1/studios", """
                {"name": "Riverside Clay", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                """), "$.id"));
        String code = read(post(owner, path("/invites"), "{\"role\": \"MEMBER\"}"), "$.code");
        assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(maker.token())).hasStatusOk();
        String booth = read(post(owner, path("/maintenance/assets"), "{\"kind\": \"SPRAY_BOOTH\", \"name\": \"Booth\"}"),
                "$.id");
        recordId = read(post(owner, path("/maintenance/assets/" + booth + "/records"), "{\"note\": \"New filter\"}"),
                "$.id");
    }

    @Test
    void staffAddPhotosToARecordThatShowOnceChecked() throws Exception {
        MvcTestResult started = post(owner, path("/maintenance/records/" + recordId + "/photos"), "");
        assertThat(started).hasStatus(HttpStatus.CREATED);
        String photoId = read(started, "$.photoId");
        // Under their own prefix in the container, apart from piece photos
        assertThat(read(started, "$.photoUploadUrl")).contains("maintenance%2F" + studioId + "%2F" + recordId + "%2F");

        // Nothing uploaded yet: refused
        assertThat(complete(photoId)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(put(read(started, "$.photoUploadUrl"), JPEG)).isEqualTo(201);
        assertThat(put(read(started, "$.thumbnailUploadUrl"), JPEG)).isEqualTo(201);
        assertThat(complete(photoId)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(complete(photoId)).hasStatus(HttpStatus.NO_CONTENT);

        String records = mvc.get().uri(path("/maintenance/assets/" + assetOf() + "/records")).with(owner.token())
                .exchange().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(records, "$[0].photos[*].id")).containsExactly(photoId);
        String url = JsonPath.read(records, "$[0].photos[0].url");
        HttpResponse<byte[]> image = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(image.statusCode()).isEqualTo(200);
        assertThat(image.body()).isEqualTo(JPEG);

        // Members can't add photos to records, and a record from nowhere isn't found
        assertThat(post(maker, path("/maintenance/records/" + recordId + "/photos"), "")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(owner, path("/maintenance/records/" + UUID.randomUUID() + "/photos"), ""))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void abandonedUploadsAreSweptAway() throws Exception {
        String photoId = read(post(owner, path("/maintenance/records/" + recordId + "/photos"), ""), "$.photoId");

        assertThat(sweep.runAt(Instant.now().plus(RecordPhotoUploadSweep.ABANDONED_AFTER).plusSeconds(60)))
                .isGreaterThanOrEqualTo(1);
        assertThat(complete(photoId)).hasStatus(HttpStatus.NOT_FOUND);
    }

    private String assetOf() throws Exception {
        return read(mvc.get().uri(path("/maintenance/assets")).with(owner.token()).exchange(), "$[0].id");
    }

    private MvcTestResult complete(String photoId) {
        return post(owner, path("/maintenance/records/" + recordId + "/photos/" + photoId + "/complete"), "");
    }

    private int put(String url, byte[] body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url))
                        .header("x-ms-blob-type", "BlockBlob")
                        .header("Content-Type", "image/jpeg")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build(), HttpResponse.BodyHandlers.discarding())
                .statusCode();
    }

    private MvcTestResult post(TestUser user, String uri, String json) {
        return mvc.post().uri(uri).with(user.token()).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private String path(String suffix) {
        return "/api/v1/studios/" + studioId + suffix;
    }

    private static String read(MvcTestResult result, String jsonPath) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
    }

}
