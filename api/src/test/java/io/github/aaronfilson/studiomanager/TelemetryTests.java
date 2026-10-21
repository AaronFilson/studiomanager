package io.github.aaronfilson.studiomanager;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The application starts and serves with telemetry on, as it runs in Azure, so a broken exporter
 * setup fails here rather than at the first deploy. Nothing is listening at the ingestion endpoint;
 * the exporter only logs that it couldn't send.
 */
@IntegrationTest
@TestPropertySource(properties = {
        "otel.sdk.disabled=false",
        "applicationinsights.connection.string=InstrumentationKey=00000000-0000-0000-0000-000000000000;"
                + "IngestionEndpoint=http://127.0.0.1:9/;LiveEndpoint=http://127.0.0.1:9/" })
class TelemetryTests {

    @Autowired
    OpenTelemetry openTelemetry;

    @Autowired
    MockMvcTester mvc;

    @Test
    void recordsSpansAndStillServesRequests() {
        Span span = openTelemetry.getTracer("test").spanBuilder("check").startSpan();
        try {
            // A no-op SDK hands out invalid span contexts
            assertThat(span.getSpanContext().isValid()).isTrue();
        } finally {
            span.end();
        }
        assertThat(mvc.get().uri("/api/v1/me").with(TestUser.named("Tess").token())).hasStatusOk();
    }

}
