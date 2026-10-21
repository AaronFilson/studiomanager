package io.github.aaronfilson.studiomanager.piece;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

/**
 * Photos through the API and a real Azurite: signed upload URLs, the checks before a photo shows,
 * who can add and remove them, the limit, and the sweep of abandoned uploads.
 */
@IntegrationTest
class PiecePhotoApiTests {

    /** Enough of a JPEG for the API's check, which reads only the first bytes. */
    private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0 };
    private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };

    private final HttpClient http = HttpClient.newHttpClient();

    @Autowired
    MockMvcTester mvc;

    @Autowired
    PhotoUploadSweep sweep;

    private final TestUser owner = TestUser.named("Olive");
    private final TestUser maker = TestUser.named("Mo");
    private final TestUser otherMaker = TestUser.named("Nia");
    private UUID studioId;
    private String pieceId;

    @BeforeEach
    void aMembersPiece() throws Exception {
        studioId = UUID.fromString(read(post(owner, "/api/v1/studios", """
                {"name": "Riverside Clay", "unitSystem": "US_CUSTOMARY", "timeZone": "America/Chicago", "country": "US"}
                """), "$.id"));
        join(maker);
        join(otherMaker);
        pieceId = read(post(maker, studioPath("/pieces"), """
                {"size": {}}
                """), "$.id");
    }

    @Test
    void aMemberUploadsStraightToStorageAndThePhotoShowsOnceChecked() throws Exception {
        MvcTestResult started = post(maker, photosPath(""), "");
        assertThat(started).hasStatus(HttpStatus.CREATED).bodyJson()
                .hasPathSatisfying("$.maxPhotoBytes", m -> assertThat(m).isEqualTo(PiecePhotoService.MAX_PHOTO_BYTES));
        String photoId = read(started, "$.photoId");

        // Not shown while pending
        assertThat(get(maker, studioPath("/pieces/" + pieceId))).bodyJson()
                .extractingPath("$.photos.length()").isEqualTo(0);

        assertThat(put(read(started, "$.photoUploadUrl"), JPEG)).isEqualTo(201);
        assertThat(put(read(started, "$.thumbnailUploadUrl"), JPEG)).isEqualTo(201);
        assertThat(complete(maker, photoId)).hasStatusOk().bodyJson()
                .hasPathSatisfying("$.photos[0].id", id -> assertThat(id).isEqualTo(photoId));

        MvcTestResult piece = get(maker, studioPath("/pieces/" + pieceId));
        String url = read(piece, "$.photos[0].url");
        HttpResponse<byte[]> image = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(image.statusCode()).isEqualTo(200);
        assertThat(image.body()).isEqualTo(JPEG);
        assertThat(image.headers().firstValue("Content-Type")).hasValue("image/jpeg");

        // The same URL on the next read, so browsers can cache the image
        assertThat(read(get(maker, studioPath("/pieces/" + pieceId)), "$.photos[0].url")).isEqualTo(url);
        // Staff see the thumbnail on the shelf
        assertThat(get(owner, studioPath("/pieces"))).bodyJson()
                .extractingPath("$[0].photos[0].thumbnailUrl").asString().contains("thumb.jpg");
        // Finishing again changes nothing
        assertThat(complete(maker, photoId)).hasStatusOk().bodyJson().extractingPath("$.photos.length()").isEqualTo(1);
    }

    @Test
    void anUploadUrlCanOnlyCreateTheImageNotReplaceIt() throws Exception {
        MvcTestResult started = post(maker, photosPath(""), "");
        String uploadUrl = read(started, "$.photoUploadUrl");

        assertThat(put(uploadUrl, JPEG)).isEqualTo(201);
        assertThat(put(uploadUrl, PNG)).isGreaterThanOrEqualTo(400);
    }

    @Test
    void refusesMissingAndNonJpegImages() throws Exception {
        MvcTestResult started = post(maker, photosPath(""), "");
        String photoId = read(started, "$.photoId");

        assertThat(complete(maker, photoId)).hasStatus(HttpStatus.BAD_REQUEST);

        put(read(started, "$.photoUploadUrl"), PNG);
        put(read(started, "$.thumbnailUploadUrl"), JPEG);
        assertThat(complete(maker, photoId)).hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
                .extractingPath("$.detail").isEqualTo("The photo isn't a JPEG image.");
    }

    @Test
    void onlyPeopleWhoCanSeeThePieceTouchItsPhotos() throws Exception {
        String photoId = addPhoto(maker);

        assertThat(post(otherMaker, photosPath(""), "")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri(photosPath("/" + photoId)).with(otherMaker.token()))
                .hasStatus(HttpStatus.NOT_FOUND);
        // Staff can add to any piece
        assertThat(post(owner, photosPath(""), "")).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void removingAPhotoDeletesItsImages() throws Exception {
        String photoId = addPhoto(maker);
        String url = read(get(maker, studioPath("/pieces/" + pieceId)), "$.photos[0].url");

        assertThat(mvc.delete().uri(photosPath("/" + photoId)).with(maker.token())).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(get(maker, studioPath("/pieces/" + pieceId))).bodyJson()
                .extractingPath("$.photos.length()").isEqualTo(0);
        assertThat(http.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.discarding())
                .statusCode()).isEqualTo(404);
    }

    @Test
    void aPieceHoldsAtMostSixPhotos() throws Exception {
        for (int i = 0; i < PiecePhotoService.MAX_PHOTOS; i++) {
            addPhoto(maker);
        }

        assertThat(post(maker, photosPath(""), "")).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void theSweepRemovesUploadsNeverFinished() throws Exception {
        MvcTestResult started = post(maker, photosPath(""), "");
        String photoId = read(started, "$.photoId");
        put(read(started, "$.photoUploadUrl"), JPEG);
        put(read(started, "$.thumbnailUploadUrl"), JPEG);

        assertThat(sweep.runAt(Instant.now().plus(PhotoUploadSweep.ABANDONED_AFTER).plusSeconds(60)))
                .isGreaterThanOrEqualTo(1);

        assertThat(complete(maker, photoId)).hasStatus(HttpStatus.NOT_FOUND);
    }

    private String addPhoto(TestUser user) throws Exception {
        MvcTestResult started = post(user, photosPath(""), "");
        String photoId = read(started, "$.photoId");
        assertThat(put(read(started, "$.photoUploadUrl"), JPEG)).isEqualTo(201);
        assertThat(put(read(started, "$.thumbnailUploadUrl"), JPEG)).isEqualTo(201);
        assertThat(complete(user, photoId)).hasStatusOk();
        return photoId;
    }

    /** What a browser does with an upload URL: a plain PUT, with no bearer token. */
    private int put(String url, byte[] body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url))
                        .header("x-ms-blob-type", "BlockBlob")
                        .header("Content-Type", "image/jpeg")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build(), HttpResponse.BodyHandlers.discarding())
                .statusCode();
    }

    private MvcTestResult complete(TestUser user, String photoId) {
        return post(user, photosPath("/" + photoId + "/complete"), "");
    }

    private void join(TestUser user) throws Exception {
        String code = read(post(owner, studioPath("/invites"), """
                {"role": "MEMBER"}
                """), "$.code");
        assertThat(mvc.post().uri("/api/v1/invites/{code}/accept", code).with(user.token())).hasStatusOk();
    }

    private MvcTestResult get(TestUser user, String path) {
        return mvc.get().uri(path).with(user.token()).exchange();
    }

    private MvcTestResult post(TestUser user, String path, String json) {
        return json.isEmpty()
                ? mvc.post().uri(path).with(user.token()).exchange()
                : mvc.post().uri(path).with(user.token()).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private String photosPath(String suffix) {
        return studioPath("/pieces/" + pieceId + "/photos" + suffix);
    }

    private String studioPath(String suffix) {
        return "/api/v1/studios/" + studioId + suffix;
    }

    private static String read(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path).toString();
    }

}
