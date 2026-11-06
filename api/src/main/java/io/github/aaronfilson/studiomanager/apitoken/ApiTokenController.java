package io.github.aaronfilson.studiomanager.apitoken;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.ApiTokenDto;
import io.github.aaronfilson.studiomanager.contract.ApiTokensApi;
import io.github.aaronfilson.studiomanager.contract.CreateApiTokenRequestDto;
import io.github.aaronfilson.studiomanager.contract.CreatedApiTokenDto;
import io.github.aaronfilson.studiomanager.contract.TokenScopeDto;
import io.github.aaronfilson.studiomanager.platform.tenancy.TokenScope;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ApiTokenController implements ApiTokensApi {

    private final ApiTokens tokens;

    ApiTokenController(ApiTokens tokens) {
        this.tokens = tokens;
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<List<ApiTokenDto>> listApiTokens(UUID studioId) {
        return ResponseEntity.ok(tokens.list().stream().map(ApiTokenController::dto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<CreatedApiTokenDto> createApiToken(UUID studioId, CreateApiTokenRequestDto request) {
        Set<TokenScope> scopes = EnumSet.noneOf(TokenScope.class);
        request.getScopes().forEach(s -> scopes.add(TokenScope.fromWireName(s.getValue()).orElseThrow()));
        var created = tokens.create(request.getName(), scopes, request.getExpiresInDays());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreatedApiTokenDto().token(dto(created.token())).secret(created.secret()));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<Void> revokeApiToken(UUID studioId, UUID tokenId) {
        tokens.revoke(tokenId);
        return ResponseEntity.noContent().build();
    }

    private static ApiTokenDto dto(ApiTokens.TokenView view) {
        return new ApiTokenDto()
                .id(view.id())
                .name(view.name())
                .ownerAccountId(view.ownerAccountId())
                .ownerName(view.ownerName())
                .scopes(view.scopes().stream().map(s -> TokenScopeDto.fromValue(s.wireName())).toList())
                .lastFour(view.lastFour())
                .createdAt(utc(view.createdAt()))
                .expiresAt(utc(view.expiresAt()))
                .lastUsedAt(utcOrNull(view.lastUsedAt()))
                .revokedAt(utcOrNull(view.revokedAt()));
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static @Nullable OffsetDateTime utcOrNull(@Nullable Instant instant) {
        return instant == null ? null : utc(instant);
    }

}
