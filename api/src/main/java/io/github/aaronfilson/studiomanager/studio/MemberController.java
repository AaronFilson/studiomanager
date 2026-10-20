package io.github.aaronfilson.studiomanager.studio;

import java.net.URI;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.CreateInviteRequestDto;
import io.github.aaronfilson.studiomanager.contract.InviteDto;
import io.github.aaronfilson.studiomanager.contract.MemberDto;
import io.github.aaronfilson.studiomanager.contract.MembersApi;
import io.github.aaronfilson.studiomanager.contract.MembershipDto;
import io.github.aaronfilson.studiomanager.contract.StudioRoleDto;
import io.github.aaronfilson.studiomanager.contract.StudioSummaryDto;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MemberController implements MembersApi {

    private final StudioMembers members;

    MemberController(StudioMembers members) {
        this.members = members;
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<List<MemberDto>> listMembers(UUID studioId) {
        return ResponseEntity.ok(members.list().stream()
                .map(member -> new MemberDto()
                        .accountId(member.accountId())
                        .displayName(member.displayName())
                        .role(StudioRoleDto.valueOf(member.role().name()))
                        .joinedAt(member.joinedAt().atOffset(ZoneOffset.UTC)))
                .toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<InviteDto> createInvite(UUID studioId, CreateInviteRequestDto request) {
        InviteView invite = members.invite(StudioRole.valueOf(request.getRole().name()));
        return ResponseEntity.created(URI.create("/api/v1/invites/" + invite.code() + "/accept"))
                .body(new InviteDto()
                        .id(invite.id())
                        .code(invite.code())
                        .role(StudioRoleDto.valueOf(invite.role().name()))
                        .expiresAt(invite.expiresAt().atOffset(ZoneOffset.UTC)));
    }

    @Override
    public ResponseEntity<MembershipDto> acceptInvite(String code) {
        MembershipView membership = members.accept(code);
        return ResponseEntity.ok(new MembershipDto()
                .studio(new StudioSummaryDto().id(membership.studioId()).name(membership.studioName()))
                .role(StudioRoleDto.valueOf(membership.role().name())));
    }

}
