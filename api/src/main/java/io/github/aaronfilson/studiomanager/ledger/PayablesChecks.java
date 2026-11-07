package io.github.aaronfilson.studiomanager.ledger;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;

/** The checks payables' use cases share, with the messages they have always given. */
final class PayablesChecks {

  private PayablesChecks() {}

  static TenantContext requireStaff() {
    TenantContext caller = TenantContext.require();
    StudioRole role = caller.role();
    if (role == null || !role.includes(StudioRole.STAFF)) {
      throw new AccessDeniedException("Only staff can do this.");
    }
    return caller;
  }

  static void requireMoney(BigDecimal amount, Currency currency) {
    if (amount.signum() <= 0) {
      throw new InvalidInputException("The amount must be more than zero.");
    }
    requireScale(amount, currency);
  }

  static void requireScale(BigDecimal amount, Currency currency) {
    if (amount.stripTrailingZeros().scale() > currency.getDefaultFractionDigits()) {
      throw new InvalidInputException(
          "Too many decimal places for " + currency.getCurrencyCode() + ".");
    }
  }

  static void requireShare(BigDecimal share) {
    if (share.signum() <= 0
        || share.compareTo(BigDecimal.ONE) > 0
        || share.stripTrailingZeros().scale() > 4) {
      throw new InvalidInputException("A share is a fraction above 0 and up to 1 (0.6 is 60%).");
    }
  }

  static LocalDate notInFuture(LocalDate day, LocalDate today) {
    if (day.isAfter(today)) {
      throw new InvalidInputException("That date hasn't happened yet.");
    }
    return day;
  }

  static @Nullable String blankToNull(@Nullable String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }

  /** Text for a journal entry's memo, which holds up to 200 characters. */
  static String truncate(String text) {
    return text.length() <= 200 ? text : text.substring(0, 200);
  }
}
