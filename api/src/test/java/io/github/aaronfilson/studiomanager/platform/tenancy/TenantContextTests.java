package io.github.aaronfilson.studiomanager.platform.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantContextTests {

  private final TenantContext caller = TenantContext.forAccount(UUID.randomUUID());

  @Test
  void isUnboundOutsideACall() {
    assertThat(TenantContext.current()).isEmpty();
    assertThatIllegalStateException().isThrownBy(TenantContext::require);
  }

  @Test
  void isBoundForExactlyTheExtentOfACall() {
    assertThat(TenantContext.callAs(caller, TenantContext::require)).isSameAs(caller);
    assertThat(TenantContext.current()).isEmpty();
  }

  @Test
  void aNestedCallRebindsAndThenRestores() {
    TenantContext inStudio = caller.inStudio(UUID.randomUUID(), StudioRole.STAFF);

    TenantContext.callAs(
        caller,
        () -> {
          assertThat(TenantContext.callAs(inStudio, TenantContext::require)).isSameAs(inStudio);
          assertThat(TenantContext.require()).isSameAs(caller);
          return null;
        });
  }

  @Test
  void aStudioAndARoleGoTogether() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TenantContext(UUID.randomUUID(), UUID.randomUUID(), null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TenantContext(UUID.randomUUID(), null, StudioRole.OWNER));
  }

  @Test
  void aJobActsOnItsOwnAccountAtItsCeiling() {
    UUID studioId = UUID.randomUUID();
    TenantContext job = TenantContext.system(SystemActor.WEBHOOK_SWEEP, studioId);
    assertThat(job.isSystem()).isTrue();
    assertThat(job.accountId()).isEqualTo(SystemActor.WEBHOOK_SWEEP.accountId());
    assertThat(job.role()).isEqualTo(StudioRole.STAFF);
    assertThat(caller.isSystem()).isFalse();
  }

  @Test
  void noJobActsAsAnOwner() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new TenantContext(
                SystemActor.WEBHOOK_SWEEP.accountId(),
                UUID.randomUUID(),
                StudioRole.OWNER,
                TenantContext.PrincipalKind.SYSTEM));
    for (SystemActor actor : SystemActor.values()) {
      assertThat(actor.ceiling().includes(StudioRole.OWNER)).as(actor.name()).isFalse();
    }
  }

  @Test
  void aJobAndAPersonCantSwapAccounts() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TenantContext.forAccount(SystemActor.ACTIVITY_CONSUMER.accountId()));
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new TenantContext(UUID.randomUUID(), null, null, TenantContext.PrincipalKind.SYSTEM));
    assertThatIllegalStateException()
        .isThrownBy(() ->
            TenantContext.system(SystemActor.ACTIVITY_CONSUMER)
                .inStudio(UUID.randomUUID(), StudioRole.MEMBER));
  }
}
