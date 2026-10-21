package io.github.aaronfilson.studiomanager.access;

import java.net.URI;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.AccessApi;
import io.github.aaronfilson.studiomanager.contract.CertificationDto;
import io.github.aaronfilson.studiomanager.contract.CertificationTypeDto;
import io.github.aaronfilson.studiomanager.contract.CreateCertificationTypeRequestDto;
import io.github.aaronfilson.studiomanager.contract.DutyDto;
import io.github.aaronfilson.studiomanager.contract.MemberDutiesDto;
import io.github.aaronfilson.studiomanager.contract.SetDutiesRequestDto;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioAuthorization;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AccessController implements AccessApi {

    private final AccessService access;
    private final StudioAuthorization authorization;

    AccessController(AccessService access, StudioAuthorization authorization) {
        this.access = access;
        this.authorization = authorization;
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<List<MemberDutiesDto>> listDuties(UUID studioId) {
        return ResponseEntity.ok(access.allDuties().entrySet().stream()
                .map(entry -> toDto(entry.getKey(), entry.getValue()))
                .toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<MemberDutiesDto> setDuties(UUID studioId, UUID accountId, SetDutiesRequestDto request) {
        Set<Duty> duties = EnumSet.noneOf(Duty.class);
        request.getDuties().forEach(duty -> duties.add(Duty.valueOf(duty.name())));
        return ResponseEntity.ok(toDto(accountId, access.setDuties(accountId, duties)));
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<List<CertificationTypeDto>> listCertificationTypes(UUID studioId) {
        return ResponseEntity.ok(access.types().stream().map(AccessController::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<CertificationTypeDto> createCertificationType(UUID studioId,
            CreateCertificationTypeRequestDto request) {
        AccessService.CertificationType type = access.createType(request.getName(), request.getValidMonths());
        return ResponseEntity.created(URI.create("/api/v1/studios/%s/certification-types/%s".formatted(studioId, type.id())))
                .body(toDto(type));
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<List<CertificationDto>> listCertifications(UUID studioId, @Nullable UUID accountId) {
        List<AccessService.Certification> found;
        if (accountId != null) {
            found = access.certifications(accountId);
        } else if (authorization.hasRole(studioId, StudioRole.STAFF)) {
            found = access.allCertifications();
        } else {
            found = access.certifications(TenantContext.require().accountId());
        }
        return ResponseEntity.ok(found.stream().map(AccessController::toDto).toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<CertificationDto> grantCertification(UUID studioId, UUID typeId, UUID accountId) {
        return ResponseEntity.ok(toDto(access.grant(typeId, accountId)));
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<Void> revokeCertification(UUID studioId, UUID typeId, UUID accountId) {
        access.revoke(typeId, accountId);
        return ResponseEntity.noContent().build();
    }

    private static MemberDutiesDto toDto(UUID accountId, Set<Duty> duties) {
        return new MemberDutiesDto().accountId(accountId)
                .duties(duties.stream().map(duty -> DutyDto.valueOf(duty.name())).toList());
    }

    private static CertificationTypeDto toDto(AccessService.CertificationType type) {
        return new CertificationTypeDto().id(type.id()).name(type.name()).validMonths(type.validMonths());
    }

    private static CertificationDto toDto(AccessService.Certification c) {
        return new CertificationDto()
                .typeId(c.typeId())
                .name(c.name())
                .accountId(c.accountId())
                .accountName(c.accountName())
                .grantedAt(c.grantedAt().atOffset(ZoneOffset.UTC))
                .expiresAt(c.expiresAt() == null ? null : c.expiresAt().atOffset(ZoneOffset.UTC))
                .valid(c.valid());
    }

}
