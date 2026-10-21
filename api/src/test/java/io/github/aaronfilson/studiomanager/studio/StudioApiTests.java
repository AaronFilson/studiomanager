package io.github.aaronfilson.studiomanager.studio;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.IntegrationTest;
import io.github.aaronfilson.studiomanager.TestUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
class StudioApiTests {

    @Autowired
    MockMvcTester mvc;

    private final TestUser olive = TestUser.named("Olive");

    @Test
    void createsAStudioWithTheCallerAsOwner() {
        MvcTestResult result = createStudio("""
                {"name": "  Riverside Clay  ", "unitSystem": "METRIC", "timeZone": "Europe/London", "country": "GB"}
                """);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).startsWith("/api/v1/studios/");
        assertThat(result).bodyJson()
                .hasPathSatisfying("$.name", name -> assertThat(name).isEqualTo("Riverside Clay"))
                .hasPathSatisfying("$.unitSystem", units -> assertThat(units).isEqualTo("METRIC"))
                .hasPathSatisfying("$.timeZone", zone -> assertThat(zone).isEqualTo("Europe/London"))
                .hasPathSatisfying("$.country", country -> assertThat(country).isEqualTo("GB"))
                .hasPathSatisfying("$.currency", currency -> assertThat(currency).isEqualTo("GBP"))
                .hasPathSatisfying("$.myRole", role -> assertThat(role).isEqualTo("OWNER"))
                .hasPathSatisfying("$.memberCount", count -> assertThat(count).isEqualTo(1));
    }

    @Test
    void takesACurrencyOtherThanTheCountrys() {
        assertThat(createStudio("""
                {"name": "Border Clay", "unitSystem": "METRIC", "timeZone": "America/Toronto",
                 "country": "CA", "currency": "USD"}
                """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.currency").isEqualTo("USD");
    }

    @Test
    void rejectsABlankName() {
        assertThat(createStudio("""
                {"name": "   ", "unitSystem": "METRIC", "timeZone": "Europe/London", "country": "GB"}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.detail").isEqualTo("A studio needs a name.");
    }

    @Test
    void rejectsAnUnknownTimeZone() {
        assertThat(createStudio("""
                {"name": "Riverside Clay", "unitSystem": "METRIC", "timeZone": "Mars/Olympus_Mons", "country": "GB"}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").asString().startsWith("Unknown time zone");
    }

    @Test
    void rejectsAnUnknownCountry() {
        assertThat(createStudio("""
                {"name": "Riverside Clay", "unitSystem": "METRIC", "timeZone": "Europe/London", "country": "XX"}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").asString().startsWith("Unknown country");
    }

    @Test
    void rejectsAnUnknownCurrency() {
        assertThat(createStudio("""
                {"name": "Riverside Clay", "unitSystem": "METRIC", "timeZone": "Europe/London",
                 "country": "GB", "currency": "XYZ"}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").asString().startsWith("Unknown currency");
    }

    @Test
    void rejectsAMissingCountry() {
        assertThat(createStudio("""
                {"name": "Riverside Clay", "unitSystem": "METRIC", "timeZone": "Europe/London"}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsAMissingUnitSystem() {
        assertThat(createStudio("""
                {"name": "Riverside Clay", "timeZone": "Europe/London", "country": "GB"}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsAMalformedStudioId() {
        assertThat(mvc.get().uri("/api/v1/studios/not-a-uuid").with(olive.token()))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void requiresAnAccessToken() {
        assertThat(mvc.get().uri("/api/v1/me"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .containsHeader(HttpHeaders.WWW_AUTHENTICATE);
    }

    @Test
    void requiresTheApiScope() {
        assertThat(mvc.get().uri("/api/v1/me").with(olive.tokenWithoutApiScope()))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    private MvcTestResult createStudio(String json) {
        return mvc.post().uri("/api/v1/studios")
                .with(olive.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

}
