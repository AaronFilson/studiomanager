package io.github.aaronfilson.studiomanager.demo;

import io.github.aaronfilson.studiomanager.contract.AdminStudioDto;
import io.github.aaronfilson.studiomanager.contract.DemoApi;
import io.github.aaronfilson.studiomanager.contract.DemoTokenDto;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * The demo studio's admin routes. The security configuration already requires the {@code
 * Platform.Admin} app role under {@code /api/v1/admin/**}; the annotations say so again.
 */
@RestController
class DemoController implements DemoApi {

  private final DemoStudio demo;

  DemoController(DemoStudio demo) {
    this.demo = demo;
  }

  @Override
  @PreAuthorize("hasAuthority('APPROLE_Platform.Admin')")
  public ResponseEntity<AdminStudioDto> resetDemoStudio() {
    DemoStudio.Reset reset = demo.reset(TenantContext.require().accountId());
    return ResponseEntity.ok(new AdminStudioDto(reset.studioId(), reset.studioName()));
  }

  @Override
  @PreAuthorize("hasAuthority('APPROLE_Platform.Admin')")
  public ResponseEntity<DemoTokenDto> createDemoToken() {
    DemoStudio.DemoToken token = demo.token();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new DemoTokenDto()
                .studioId(token.studioId())
                .studioName(token.studioName())
                .name(token.name())
                .secret(token.secret())
                .expiresAt(token.expiresAt().atOffset(ZoneOffset.UTC)));
  }
}
