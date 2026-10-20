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

        TenantContext.callAs(caller, () -> {
            assertThat(TenantContext.callAs(inStudio, TenantContext::require)).isSameAs(inStudio);
            assertThat(TenantContext.require()).isSameAs(caller);
            return null;
        });
    }

    @Test
    void aStudioAndARoleGoTogether() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TenantContext(UUID.randomUUID(), UUID.randomUUID(), null));
        assertThatIllegalArgumentException().isThrownBy(() -> new TenantContext(UUID.randomUUID(), null, StudioRole.OWNER));
    }

}
