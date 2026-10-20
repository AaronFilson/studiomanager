package io.github.aaronfilson.studiomanager.platform.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StudioRoleTests {

    @Test
    void eachRoleIncludesTheRolesBelowIt() {
        assertThat(StudioRole.OWNER.includes(StudioRole.STAFF)).isTrue();
        assertThat(StudioRole.OWNER.includes(StudioRole.MEMBER)).isTrue();
        assertThat(StudioRole.STAFF.includes(StudioRole.MEMBER)).isTrue();
        assertThat(StudioRole.MEMBER.includes(StudioRole.MEMBER)).isTrue();
    }

    @Test
    void noRoleIncludesTheRolesAboveIt() {
        assertThat(StudioRole.MEMBER.includes(StudioRole.STAFF)).isFalse();
        assertThat(StudioRole.STAFF.includes(StudioRole.OWNER)).isFalse();
    }

}
