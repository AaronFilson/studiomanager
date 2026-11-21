package io.github.aaronfilson.studiomanager.account;

import io.github.aaronfilson.studiomanager.contract.AccountApi;
import io.github.aaronfilson.studiomanager.contract.AccountDto;
import io.github.aaronfilson.studiomanager.contract.LinkMcpIdentityRequestDto;
import io.github.aaronfilson.studiomanager.contract.LinkedIdentityDto;
import io.github.aaronfilson.studiomanager.contract.MeDto;
import io.github.aaronfilson.studiomanager.contract.MembershipDto;
import io.github.aaronfilson.studiomanager.contract.StudioRoleDto;
import io.github.aaronfilson.studiomanager.contract.StudioSummaryDto;
import io.github.aaronfilson.studiomanager.platform.tenancy.PlatformAdmin;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.MembershipView;
import io.github.aaronfilson.studiomanager.studio.StudioService;
import java.net.URI;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MeController implements AccountApi {

  private final AccountService accounts;
  private final StudioService studios;

  MeController(AccountService accounts, StudioService studios) {
    this.accounts = accounts;
    this.studios = studios;
  }

  @Override
  public ResponseEntity<MeDto> getMe() {
    UUID accountId = TenantContext.require().accountId();
    Account account = accounts.get(accountId);
    AccountDto accountDto =
        new AccountDto(account.id()).displayName(account.displayName()).email(account.email());
    return ResponseEntity.ok(
        new MeDto(
                accountDto,
                studios.membershipsOf(accountId).stream().map(MeController::toDto).toList())
            .platformAdmin(PlatformAdmin.callerHasRole()));
  }

  @Override
  public ResponseEntity<List<LinkedIdentityDto>> listMyIdentities() {
    UUID accountId = requireInteractiveAccount();
    return ResponseEntity.ok(
        accounts.listIdentities(accountId).stream().map(MeController::toDto).toList());
  }

  @Override
  public ResponseEntity<LinkedIdentityDto> linkMcpIdentity(LinkMcpIdentityRequestDto request) {
    UUID accountId = requireInteractiveAccount();
    AccountService.LinkedIdentity linked = accounts.linkMcpIdentity(accountId, request.getToken());
    return ResponseEntity.created(URI.create("/api/v1/me/identities/" + linked.id()))
        .body(toDto(linked));
  }

  @Override
  public ResponseEntity<Void> unlinkMyIdentity(UUID identityId) {
    UUID accountId = requireInteractiveAccount();
    accounts.unlinkIdentity(accountId, identityId);
    return ResponseEntity.noContent().build();
  }

  private static UUID requireInteractiveAccount() {
    TenantContext caller = TenantContext.require();
    if (caller.viaTokenId() != null) {
      throw new AccessDeniedException(
          "Manage linked identities while signed in, not with an API token.");
    }
    return caller.accountId();
  }

  private static LinkedIdentityDto toDto(AccountService.LinkedIdentity identity) {
    return new LinkedIdentityDto(
        identity.id(),
        identity.issuer(),
        identity.subject(),
        identity.createdAt().atOffset(ZoneOffset.UTC));
  }

  private static MembershipDto toDto(MembershipView membership) {
    return new MembershipDto(
        new StudioSummaryDto(membership.studioId(), membership.studioName()),
        StudioRoleDto.valueOf(membership.role().name()));
  }
}
