package io.github.aaronfilson.studiomanager.account;

import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.AccountApi;
import io.github.aaronfilson.studiomanager.contract.AccountDto;
import io.github.aaronfilson.studiomanager.contract.MeDto;
import io.github.aaronfilson.studiomanager.contract.MembershipDto;
import io.github.aaronfilson.studiomanager.contract.StudioRoleDto;
import io.github.aaronfilson.studiomanager.contract.StudioSummaryDto;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.MembershipView;
import io.github.aaronfilson.studiomanager.studio.StudioService;

import org.springframework.http.ResponseEntity;
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
        AccountDto accountDto = new AccountDto(account.id()).displayName(account.displayName()).email(account.email());
        return ResponseEntity.ok(new MeDto(accountDto,
                studios.membershipsOf(accountId).stream().map(MeController::toDto).toList()));
    }

    private static MembershipDto toDto(MembershipView membership) {
        return new MembershipDto(new StudioSummaryDto(membership.studioId(), membership.studioName()),
                StudioRoleDto.valueOf(membership.role().name()));
    }

}
