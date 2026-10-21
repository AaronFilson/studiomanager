package io.github.aaronfilson.studiomanager.studio;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.CreateInviteRequestDto;
import io.github.aaronfilson.studiomanager.contract.EmailInvitesRequestDto;
import io.github.aaronfilson.studiomanager.contract.EmailInvitesResultDto;
import io.github.aaronfilson.studiomanager.contract.InviteDto;
import io.github.aaronfilson.studiomanager.contract.MemberDto;
import io.github.aaronfilson.studiomanager.contract.MembersApi;
import io.github.aaronfilson.studiomanager.contract.MembershipDto;
import io.github.aaronfilson.studiomanager.contract.OpenInviteDto;
import io.github.aaronfilson.studiomanager.contract.SkippedEmailDto;
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
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<List<OpenInviteDto>> listOpenInvites(UUID studioId) {
        Instant now = Instant.now();
        return ResponseEntity.ok(members.openInvites().stream().map(invite -> toDto(invite, now)).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<EmailInvitesResultDto> emailInvites(UUID studioId, EmailInvitesRequestDto request) {
        EmailInvitesResult result = members.emailInvites(request.getText(),
                StudioRole.valueOf(request.getRole().name()));
        Instant now = Instant.now();
        return ResponseEntity.ok(new EmailInvitesResultDto()
                .invited(result.invited().stream().map(invite -> toDto(invite, now)).toList())
                .skipped(result.skipped().stream()
                        .map(skip -> new SkippedEmailDto()
                                .email(skip.email())
                                .reason(SkippedEmailDto.ReasonEnum.valueOf(skip.reason().name())))
                        .toList()));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<Void> revokeInvite(UUID studioId, UUID inviteId) {
        members.revoke(inviteId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<OpenInviteDto> resendInvite(UUID studioId, UUID inviteId) {
        return ResponseEntity.ok(toDto(members.resend(inviteId), Instant.now()));
    }

    @Override
    public ResponseEntity<MembershipDto> acceptInvite(String code) {
        MembershipView membership = members.accept(code);
        return ResponseEntity.ok(new MembershipDto()
                .studio(new StudioSummaryDto().id(membership.studioId()).name(membership.studioName()))
                .role(StudioRoleDto.valueOf(membership.role().name())));
    }

    private static OpenInviteDto toDto(OpenInviteView invite, Instant now) {
        return new OpenInviteDto()
                .id(invite.id())
                .email(invite.email())
                .role(StudioRoleDto.valueOf(invite.role().name()))
                .createdAt(invite.createdAt().atOffset(ZoneOffset.UTC))
                .expiresAt(invite.expiresAt().atOffset(ZoneOffset.UTC))
                .expired(invite.expiredAt(now))
                .lastSentAt(invite.lastSentAt() == null ? null : invite.lastSentAt().atOffset(ZoneOffset.UTC));
    }

}
