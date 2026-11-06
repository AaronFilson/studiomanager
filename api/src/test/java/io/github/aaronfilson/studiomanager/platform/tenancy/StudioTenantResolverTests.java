package io.github.aaronfilson.studiomanager.platform.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aaronfilson.studiomanager.platform.Uuids;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StudioTenantResolverTests {

  private final StudioTenantResolver resolver = new StudioTenantResolver();
  private final UUID accountId = Uuids.v7();

  @Test
  void aBoundStudioIsTheTenant() {
    UUID studioId = Uuids.v7();
    TenantContext inStudio = new TenantContext(accountId, studioId, StudioRole.MEMBER);

    assertThat(TenantContext.callAs(inStudio, resolver::resolveCurrentTenantIdentifier))
        .isEqualTo(studioId);
  }

  @Test
  void withNoStudioBoundTheTenantMatchesNoStudio() {
    assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(StudioTenantResolver.NO_STUDIO);
    assertThat(
            TenantContext.callAs(
                TenantContext.forAccount(accountId), resolver::resolveCurrentTenantIdentifier))
        .isEqualTo(StudioTenantResolver.NO_STUDIO);
    assertThat(
            TenantContext.callAs(
                TenantContext.system(SystemActor.values()[0]),
                resolver::resolveCurrentTenantIdentifier))
        .isEqualTo(StudioTenantResolver.NO_STUDIO);
  }

  @Test
  void noStudioIsNeverAStudioId() {
    // Studio ids are UUIDv7; the nil UUID has version 0
    assertThat(StudioTenantResolver.NO_STUDIO.version()).isZero();
    assertThat(Uuids.v7().version()).isEqualTo(7);
  }
}
